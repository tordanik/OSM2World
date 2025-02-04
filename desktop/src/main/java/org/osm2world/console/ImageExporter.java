package org.osm2world.console;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.osm2world.console.CLIArgumentsUtil.getOutputMode;
import static org.osm2world.console.CLIArgumentsUtil.getResolution;
import static org.osm2world.core.conversion.ConfigUtil.*;
import static org.osm2world.core.target.jogl.JOGLRenderingParameters.Winding.CCW;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import org.osm2world.console.CLIArgumentsUtil.OutputMode;
import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.conversion.ConfigUtil;
import org.osm2world.core.conversion.O2WConfig;
import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.jogl.*;
import org.osm2world.core.util.Resolution;

import com.jogamp.opengl.*;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineByte;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.PngChunkTextVar;
import ar.com.hjg.pngj.chunks.PngMetadata;

public class ImageExporter {

	/**
	 * the width and height of the canvas used for rendering the exported image
	 * each must not exceed the canvas limit. If the requested image is larger,
	 * it will be rendered in multiple passes and combined afterwards.
	 * This is intended to avoid overwhelmingly large canvases
	 * (which would lead to crashes)
	 */
	private static final int DEFAULT_CANVAS_LIMIT = 1024;

	private final Results results;
	private final O2WConfig config;

	private File backgroundImage;
	private JOGLTextureManager backgroundTextureManager;

	private final Color clearColor;
	private final boolean exportAlpha;

	private GLOffscreenAutoDrawable drawable;
	private ImageExporterGLEventListener listener;
	private final int pBufferSizeX;
	private final int pBufferSizeY;

	/** target prepared in init; null for unbuffered rendering */
	private JOGLTarget bufferTarget = null;

	private boolean unbufferedRendering;


	/**
	 * Creates an {@link ImageExporter} for later use.
	 * Already performs calculations that only need to be done once for a group of files.
	 */
	private ImageExporter(O2WConfig config, Results results,
						  int pBufferSizeX, int pBufferSizeY, boolean unbufferedRendering) {

		this.results = results;
		this.config = config;

		this.pBufferSizeX = pBufferSizeX;
		this.pBufferSizeY = pBufferSizeY;
		this.unbufferedRendering = unbufferedRendering;

		/* warn about potentially oversized canvas dimensions */

		int canvasLimit = config.getInt(CANVAS_LIMIT_KEY, DEFAULT_CANVAS_LIMIT);

		if (pBufferSizeX > canvasLimit || pBufferSizeY > canvasLimit) {
			System.err.println("Warning: Canvas for image export may be too large for the system's capabilities");
		}

		/* parse background color/image and other configuration options */

		this.exportAlpha = config.getBoolean("exportAlpha", false);

		Color bgColor = parseColor(config.getString(BG_COLOR_KEY), Color.BLACK);
		if (exportAlpha) {
			this.clearColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 0);
		} else {
			this.clearColor = bgColor;
		}

		if (config.containsKey(BG_IMAGE_KEY)) {
			backgroundImage = ConfigUtil.resolveFileConfigProperty(config, config.getString(BG_IMAGE_KEY));
			if (backgroundImage == null || !backgroundImage.exists()) {
				System.err.println("background image file doesn't exist: "
						+ backgroundImage);
				backgroundImage = null;
			}
		}

		/* create GL canvas and set rendering parameters */

		GLProfile profile;
		if ("shader".equals(config.getString("joglImplementation"))) {
			profile = GLProfile.get(GLProfile.GL3);
		} else {
			profile = GLProfile.get(GLProfile.GL2);
		}

		GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);

		if (! factory.canCreateGLPbuffer(null, profile) && ! factory.canCreateFBO(null, profile)) {
			throw new Error("Cannot create GLPbuffer or FBO for OpenGL output!");
		}

		GLCapabilities cap = new GLCapabilities(profile);
		cap.setDoubleBuffered(false);
		if (exportAlpha)
			cap.setAlphaBits(8);

		// set MSAA (Multi Sample Anti-Aliasing)
		int msaa = config.getInt("msaa", 0);
		if (msaa > 0) {
			cap.setSampleBuffers(true);
			cap.setNumSamples(msaa);
		}

		if ("shader".equals(config.getString("joglImplementation"))) {

			if ("shadowVolumes".equals(config.getString("shadowImplementation"))
					|| "both".equals(config.getString("shadowImplementation"))) {
				cap.setStencilBits(8);
			}
		}

		drawable = factory.createOffscreenAutoDrawable(null,
				cap, null, pBufferSizeX, pBufferSizeY);
		listener = new ImageExporterGLEventListener();
		drawable.addGLEventListener(listener);

		backgroundTextureManager = new JOGLTextureManager(drawable.getGL());

	}

	/**
	 * Creates an {@link ImageExporter} for later use.
	 * Also performs calculations that only need to be done once for a group
	 * of files, based on a {@link CLIArgumentsGroup}.
	 *
	 * @param expectedGroup  group that should contain at least the arguments
	 *                       for the files that will later be requested.
	 *                       Basis for optimization preparations.
	 */
	public static ImageExporter create(O2WConfig config, Results results,
			CLIArgumentsGroup expectedGroup) {

		int canvasLimit = config.getInt(CANVAS_LIMIT_KEY, DEFAULT_CANVAS_LIMIT);

		/* find out what number and size of image file requests to expect */

		int expectedFileCalls = 0;
		int expectedMaxSizeX = 1;
		int expectedMaxSizeY = 1;
		boolean perspectiveProjection = false;

		for (CLIArguments args : expectedGroup.getCLIArgumentsList()) {

			for (File outputFile : args.getOutput()) {
				OutputMode outputMode = getOutputMode(outputFile);
				if (outputMode == OutputMode.PNG || outputMode == OutputMode.PPM || outputMode == OutputMode.GD) {
					expectedFileCalls += 1;
					expectedMaxSizeX = max(expectedMaxSizeX, getResolution(args).width);
					expectedMaxSizeY = max(expectedMaxSizeY, getResolution(args).height);
					perspectiveProjection |= args.isPviewPos();
				}
			}

		}

		boolean onlyOneRenderPass = (expectedFileCalls <= 1
				&& expectedMaxSizeX <= canvasLimit
				&& expectedMaxSizeY <= canvasLimit);

		/* call the constructor */

		boolean unbufferedRendering = onlyOneRenderPass
				|| config.getBoolean("forceUnbufferedPNGRendering", false);

		int pBufferSizeX, pBufferSizeY;

		if (perspectiveProjection) {
			pBufferSizeX = expectedMaxSizeX;
			pBufferSizeY = expectedMaxSizeY;
		} else {
			pBufferSizeX = min(canvasLimit, expectedMaxSizeX);
			pBufferSizeY = min(canvasLimit, expectedMaxSizeY);
		}

		return new ImageExporter(config, results, pBufferSizeX, pBufferSizeY, unbufferedRendering);

	}

	/**
	 * Creates an {@link ImageExporter} for later use.
	 * Performance-related parameters are set such that they work best for rendering a single image.
	 *
	 * @param canvasResolution  the maximum resolution of the internal rendering canvas.
	 *   This is the maximum size of images that can be rendered in a single pass.
	 *   (Images with an orthographic projection can be automatically split and rendered in multiple passes,
	 *   but those with a perspective projection need to be rendered all at once.)
	 *   If the value is too high for the system's capabilities, OSM2World may crash.
	 */
	public static ImageExporter create(O2WConfig config, Results results,
									   Resolution canvasResolution) {

		boolean unbufferedRendering = true;

		int pBufferSizeX = canvasResolution.width;
		int pBufferSizeY = canvasResolution.height;

		return new ImageExporter(config, results, pBufferSizeX, pBufferSizeY, unbufferedRendering);

	}

	@Override
	protected void finalize() throws Throwable {
		freeResources();
	}

	/**
	 * manually frees resources that would otherwise remain used
	 * until the finalize call. It is no longer possible to use
	 * {@link #writeImageFile(File, CLIArgumentsUtil.OutputMode, int, int, Camera, Projection)}
	 * afterwards.
	 */
	public void freeResources() {

		if (backgroundTextureManager != null) {
			backgroundTextureManager.releaseAll();
			backgroundTextureManager = null;
		}

		if (bufferTarget != null) {
			bufferTarget.freeResources();
			bufferTarget = null;
		}

		if (drawable != null) {
			drawable.destroy();
			drawable = null;
		}

	}

	/**
	 * renders this ImageExporter's content to a file
	 *
	 * @param outputMode   one of the image output modes
	 * @param x            horizontal resolution
	 * @param y            vertical resolution
	 */
	public void writeImageFile(
			File outputFile, OutputMode outputMode,
			int x, int y,
			final Camera camera,
			final Projection projection) throws IOException {

		/* FIXME: this would be needed for cases where BufferSizes are so unbeliveable large that the temp images go beyond the memory limit
		while (((1<<31)/x) <= pBufferSizeY) {
			pBufferSizeY /= 2;
		}
		*/

		listener.prepareRendering(camera, projection, x, y);

		/* determine the number of "parts" to split the rendering in */

		int xParts = 1 + ((x-1) / pBufferSizeX);
		int yParts = 1 + ((y-1) / pBufferSizeY);

		/* generate ImageWriter */
		ImageWriter imageWriter;

		switch (outputMode) {
		case PNG: imageWriter = new PNGWriter(outputFile, x, y, exportAlpha); break;
		case PPM: imageWriter = new PPMWriter(outputFile, x, y); break;
		case GD: imageWriter = new GDWriter(outputFile, x, y); break;

		default: throw new IllegalArgumentException(
				"output mode not supported " + outputMode);
		}

		/* create image (maybe in multiple parts) */

        BufferedImage image = new BufferedImage(x, pBufferSizeY, exportAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);

		Graphics2D graphics = image.createGraphics();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));

        for (int yPart = yParts-1; yPart >=0 ; --yPart) {

        	int yStart = yPart * pBufferSizeY;
			int yEnd   = (yPart+1 < yParts) ? (yStart + (pBufferSizeY-1)) : (y-1);
			int ySize  = (yEnd - yStart) + 1;

        	for (int xPart = 0; xPart < xParts; ++xPart) {

				/* calculate start, end and size (in pixels)
				 * of the image part that will be rendered in this pass */

				int xStart = xPart * pBufferSizeX;
				int xEnd   = (xPart+1 < xParts) ? (xStart + (pBufferSizeX-1)) : (x-1);
				int xSize  = (xEnd - xStart) + 1;

				listener.setPart(xStart, yStart, xEnd, yEnd, xSize, ySize);

				// render everything
        		drawable.display();

				/* make screenshot and paste into the buffer that will contain
				 * pBufferSizeY entire image lines */

        		drawable.getContext().makeCurrent();
				AWTGLReadBufferUtil reader = new AWTGLReadBufferUtil(drawable.getGLProfile(), exportAlpha);
				BufferedImage imagePart = reader.readPixelsToBufferedImage(drawable.getGL(), 0, 0, xSize, ySize, true);
        		drawable.getContext().release();

        		graphics.drawImage(imagePart, xStart, 0, xSize, ySize, null);
			}

        	imageWriter.append(image, ySize);
		}

        imageWriter.close();
	}

	private static JOGLTarget createJOGLTarget(GL gl, Results results,
			O2WConfig config) {

		JOGLTarget target;
		if ("shader".equals(config.getString("joglImplementation"))) {
			boolean drawBoundingBox = config.getBoolean("drawBoundingBox", false);
			boolean shadowVolumes = "shadowVolumes".equals(config.getString("shadowImplementation"))
					|| "both".equals(config.getString("shadowImplementation"));
			boolean shadowMaps = "shadowMap".equals(config.getString("shadowImplementation"))
					|| "both".equals(config.getString("shadowImplementation"));
			int shadowMapWidth = config.getInt("shadowMapWidth", 4096);
			int shadowMapHeight = config.getInt("shadowMapHeight", 4096);
			int shadowMapCameraFrustumPadding = config.getInt("shadowMapCameraFrustumPadding", 8);
			boolean useSSAO = "true".equals(config.getString("useSSAO"));
			int SSAOkernelSize = config.getInt("SSAOkernelSize", 16);
			float SSAOradius = config.getFloat("SSAOradius", 1);
			boolean overwriteProjectionClippingPlanes = "true".equals(config.getString("overwriteProjectionClippingPlanes"));
			target = new JOGLTargetShader(gl.getGL3(),
					new JOGLRenderingParameters(CCW, false, true, drawBoundingBox, shadowVolumes, shadowMaps, shadowMapWidth, shadowMapHeight,
			    			shadowMapCameraFrustumPadding, useSSAO, SSAOkernelSize, SSAOradius, overwriteProjectionClippingPlanes),
					GlobalLightingParameters.DEFAULT);
		} else {
			target = new JOGLTargetFixedFunction(gl.getGL2(),
					new JOGLRenderingParameters(CCW, false, true),
					GlobalLightingParameters.DEFAULT);
		}


		target.setConfiguration(config);

		boolean underground = config.getBoolean("renderUnderground", true);

		target.setXZBoundary(results.getMapData().getBoundary());
		TargetUtil.renderWorldObjects(target, results.getMapData(), underground);

		target.finish();

		return target;

	}


	/**
	 * interface ImageWriter is used to abstract the underlaying image
	 * format. It can be used for incremental image writes of huge images
	 */
	public static interface ImageWriter {
		void append(BufferedImage img) throws IOException;
		void append(BufferedImage img, int lines) throws IOException;
		void close() throws IOException;
	}

	/**
	 * Implementation of an ImageWriter to write png files
	 */
	public static class PNGWriter implements ImageWriter {

		private ImageInfo imgInfo;
		private PngWriter writer;

		public PNGWriter(File outputFile, int cols, int rows, boolean alpha) {
			imgInfo = new ImageInfo(cols, rows, 8, alpha);
			writer = new PngWriter(outputFile, imgInfo, true);

			PngMetadata metaData = writer.getMetadata();
			metaData.setTimeNow();
			metaData.setText(PngChunkTextVar.KEY_Software, "OSM2World");
		}

		@Override
		public void append(BufferedImage img) throws IOException {
			append(img, img.getHeight());
		}

		@Override
		public void append(BufferedImage img, int lines) throws IOException {

			/* get raw data of image */
			DataBuffer imageDataBuffer = img.getRaster().getDataBuffer();
			int[] data = (((DataBufferInt)imageDataBuffer).getData());

			/* create one ImageLine that will be refilled and written to png */
			ImageLineByte bline = new ImageLineByte(imgInfo);
			byte[] line = bline.getScanline();
			int channels = imgInfo.channels;

			for (int i = 0; i < lines; i++) {
				for (int d = 0; d < img.getWidth(); d++) {
					int val = data[i*img.getWidth()+d];
					line[channels*d+0] = (byte) (val >> 16);
					line[channels*d+1] = (byte) (val >> 8);
					line[channels*d+2] = (byte) val;
					if (channels > 3)
						line[channels*d+3] = (byte) (val >> 24);
				}
				writer.writeRow(bline);
			}
		}

		@Override
		public void close() throws IOException {
			writer.end();
			writer.close();
		}
	}

	/**
	 * Implementation of an ImageWriter to write raw ppm files
	 */
	public static class PPMWriter implements ImageWriter {

		private FileOutputStream out;
		private FileChannel fc;
		private File outputFile;
		private int cols;
		private int rows;

		public PPMWriter(File outputFile, int cols, int rows) {
			this.cols = cols;
			this.rows = rows;
			this.outputFile = outputFile;
		}

		private void writeHeader() throws IOException {

			out = new FileOutputStream(outputFile);

			// write header
			Charset charSet = Charset.forName("US-ASCII");
			out.write("P6\n".getBytes(charSet));
			out.write(String.format("%d %d\n", cols, rows).getBytes(charSet));
			out.write("255\n".getBytes(charSet));

			fc = out.getChannel();
		}


		@Override
		public void append(BufferedImage img) throws IOException {
			append(img, img.getHeight());
		}

		@Override
		public void append(BufferedImage img, int lines) throws IOException {

			if (fc == null) {
				writeHeader();
			}

			// collect and write content

			ByteBuffer writeBuffer = ByteBuffer.allocate(
					3 * img.getWidth() * lines);

			DataBuffer imageDataBuffer = img.getRaster().getDataBuffer();
			int[] data = (((DataBufferInt)imageDataBuffer).getData());

			for (int i = 0; i < img.getWidth() * lines; i++) {
				int value = data[i];
				writeBuffer.put((byte)(value >>> 16));
				writeBuffer.put((byte)(value >>> 8));
				writeBuffer.put((byte)(value));
			}

			writeBuffer.position(0);
			fc.write(writeBuffer);

		}

		@Override
		public void close() throws IOException {

			if (fc != null) {
				fc.close();
			}

			if (out != null) {
				out.close();
			}
		}
	}

	/**
	 * Implementation of an ImageWriter to write the (rare) gd file format
	 */
	public static class GDWriter implements ImageWriter {

		//TODO: dimensions are limited to short!

		private FileOutputStream out;
		private FileChannel fc;
		private File outputFile;
		private int cols;
		private int rows;

		public GDWriter(File outputFile, int cols, int rows) {
			this.cols = cols;
			this.rows = rows;
			this.outputFile = outputFile;
		}

		private void writeHeader() throws IOException {

			out = new FileOutputStream(outputFile);

			out.write(0xff);
			out.write(0xfe);

			//write dimensions
			DataOutputStream dOut = new DataOutputStream(out);
			dOut.writeShort(cols);
			dOut.writeShort(rows);

			out.write(0x01);
			out.write(0xff);
			out.write(0xff);
			out.write(0xff);
			out.write(0xff);
			out.write(0x00);

			fc = out.getChannel();
		}


		@Override
		public void append(BufferedImage img) throws IOException {
			append(img, img.getHeight());
		}

		@Override
		public void append(BufferedImage img, int lines) throws IOException {

			if (fc == null) {
				writeHeader();
			}

			// collect and write content

			ByteBuffer writeBuffer = ByteBuffer.allocate(
					4 * img.getWidth() * lines);

			DataBuffer imageDataBuffer = img.getRaster().getDataBuffer();
			int[] data = (((DataBufferInt)imageDataBuffer).getData());

			for (int i = 0; i < img.getWidth() * lines; i++) {
				int value = data[i];
				writeBuffer.put((byte)(value >>> 16));
				writeBuffer.put((byte)(value >>> 8));
				writeBuffer.put((byte)(value));
				writeBuffer.put((byte) 0);
			}

			writeBuffer.position(0);
			fc.write(writeBuffer);

		}

		@Override
		public void close() throws IOException {

			if (fc != null) {
				fc.close();
			}

			if (out != null) {
				out.close();
			}
		}
	}

	public class ImageExporterGLEventListener implements GLEventListener {
		private int xStart;
		private int yStart;
		private int xEnd;
		private int yEnd;
		private int xSize;
		private int ySize;
		private int x;
		private int y;
		private Camera camera;
		private Projection projection;
		private boolean nodisplay = false;

		@Override
		public void init(GLAutoDrawable drawable) {

			/* render map data into buffer if it needs to be rendered multiple times */
			if (!unbufferedRendering ) {
				bufferTarget = createJOGLTarget(drawable.getGL(), results, config);
			}
		}

		public void setPart(int xStart, int yStart, int xEnd, int yEnd,
				int xSize, int ySize) {

			if (this.xSize != xSize || this.ySize != ySize) {
				// disable display while resizing. all display calls need to be from @writeImageFile
				nodisplay = true;
				drawable.setSurfaceSize(xSize, ySize);
				nodisplay = false;
			}

			this.xStart = xStart;
			this.yStart = yStart;
			this.xEnd = xEnd;
			this.yEnd = yEnd;
			this.xSize = xSize;
			this.ySize = ySize;
		}

		public void prepareRendering(Camera camera, Projection projection,
				int x, int y) {
			this.camera = camera;
			this.projection = projection;
			this.x = x;
			this.y = y;
		}

		@Override
		public void dispose(GLAutoDrawable drawable) {
		}

		@Override
		public void display(GLAutoDrawable drawable) {

			if (nodisplay)
				return;

			/* configure rendering */

			AbstractJOGLTarget.clearGL(drawable.getGL(), clearColor);

			/* render to pBuffer */

			JOGLTarget target = (bufferTarget == null)?
					createJOGLTarget(drawable.getGL(), results, config) : bufferTarget;

					if (backgroundImage != null) {
						target.drawBackgoundImage(backgroundImage,
								xStart, yStart, xSize, ySize,
								backgroundTextureManager);
					}

					target.renderPart(camera, projection,
							xStart / (double)(x-1), xEnd / (double)(x-1),
							yStart / (double)(y-1), yEnd / (double)(y-1));

					if (target != bufferTarget) {
						target.freeResources();
					}
		}

		@Override
		public void reshape(GLAutoDrawable drawable, int x, int y, int width,
				int height) {
		}
	}
}
