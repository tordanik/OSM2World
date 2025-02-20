package org.osm2world.output.image;

import static org.osm2world.output.jogl.JOGLRenderingParameters.Winding.CCW;

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
import java.util.function.Consumer;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.output.common.lighting.GlobalLightingParameters;
import org.osm2world.output.common.rendering.Camera;
import org.osm2world.output.common.rendering.Projection;
import org.osm2world.output.jogl.*;
import org.osm2world.util.Resolution;

import com.jogamp.opengl.*;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineByte;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.PngChunkTextVar;
import ar.com.hjg.pngj.chunks.PngMetadata;

public class ImageExporter {

	private final AxisAlignedRectangleXZ dataBbox;
	private final Consumer<JOGLOutput> renderToTarget;
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
	private JOGLOutput bufferTarget = null;

	private boolean unbufferedRendering;


	/**
	 * Creates an {@link ImageExporter} for later use.
	 * Already performs calculations that only need to be done once for a group of files.
	 */
	private ImageExporter(O2WConfig config, AxisAlignedRectangleXZ dataBbox, Consumer<JOGLOutput> renderToTarget,
						  Resolution canvasResolution, boolean unbufferedRendering) {

		this.dataBbox = dataBbox;
		this.config = config;
		this.renderToTarget = renderToTarget;

		this.pBufferSizeX = canvasResolution.width;
		this.pBufferSizeY = canvasResolution.height;
		this.unbufferedRendering = unbufferedRendering;

		/* warn about potentially oversize canvas dimensions */

		int canvasLimit = config.canvasLimit();

		if (pBufferSizeX > canvasLimit || pBufferSizeY > canvasLimit) {
			System.err.println("Warning: Canvas for image export may be too large for the system's capabilities");
		}

		/* parse background color/image and other configuration options */

		this.exportAlpha = config.getBoolean("exportAlpha", false);

		Color bgColor = config.backgroundColor();
		if (exportAlpha) {
			this.clearColor = new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 0);
		} else {
			this.clearColor = bgColor;
		}

		if (config.containsKey("backgroundImage")) {
			backgroundImage = config.resolveFileConfigProperty(config.getString("backgroundImage"));
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
	 *
	 * @param renderToTarget  function which will render the geometry that should be visible on the created images
	 * @param canvasResolution  the maximum resolution of the internal rendering canvas.
	 *   This is the maximum size of images that can be rendered in a single pass.
	 *   (Images with an orthographic projection can be automatically split and rendered in multiple passes,
	 *   but those with a perspective projection need to be rendered all at once.)
	 *   If the value is too high for the system's capabilities, OSM2World may crash.
	 */
	public static ImageExporter create(O2WConfig config, AxisAlignedRectangleXZ dataBbox,
			Consumer<JOGLOutput> renderToTarget, Resolution canvasResolution, boolean unbufferedRendering) {
		return new ImageExporter(config, dataBbox, renderToTarget, canvasResolution, unbufferedRendering);
	}

	/**
	 * Creates an {@link ImageExporter} for later use.
	 * Performance-related parameters are set such that they work best for rendering a single image.
	 */
	public static ImageExporter create(O2WConfig config, AxisAlignedRectangleXZ dataBbox,
			Consumer<JOGLOutput> renderToTarget, Resolution canvasResolution) {
		return create(config, dataBbox, renderToTarget, canvasResolution, true);
	}

	@Override
	protected void finalize() throws Throwable {
		freeResources();
	}

	/**
	 * manually frees resources that would otherwise remain used
	 * until the finalize call. It is no longer possible to use
	 * {@link #writeImageFile(File, ImageOutputFormat, int, int, Camera, Projection)}
	 * afterward.
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
	 * @param imageFormat  image format
	 * @param x            horizontal resolution
	 * @param y            vertical resolution
	 */
	public void writeImageFile(
			File outputFile, ImageOutputFormat imageFormat,
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

		ImageWriter imageWriter = switch (imageFormat) {
			case PNG -> new PNGWriter(outputFile, x, y, exportAlpha);
			case PPM -> new PPMWriter(outputFile, x, y);
			case GD -> new GDWriter(outputFile, x, y);
		};

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

	private static JOGLOutput createJOGLTarget(GL gl, AxisAlignedRectangleXZ dataBbox,
			Consumer<JOGLOutput> renderToTarget, O2WConfig config) {

		JOGLOutput target;
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
			target = new JOGLOutputShader(gl.getGL3(),
					new JOGLRenderingParameters(CCW, false, true, drawBoundingBox, shadowVolumes, shadowMaps, shadowMapWidth, shadowMapHeight,
			    			shadowMapCameraFrustumPadding, useSSAO, SSAOkernelSize, SSAOradius, overwriteProjectionClippingPlanes),
					GlobalLightingParameters.DEFAULT);
		} else {
			target = new JOGLOutputFixedFunction(gl.getGL2(),
					new JOGLRenderingParameters(CCW, false, true),
					GlobalLightingParameters.DEFAULT);
		}


		target.setConfiguration(config);

		target.setXZBoundary(dataBbox);
		renderToTarget.accept(target);

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

	/** parameters for optimizing the performance of an {@link ImageExporter} */
	public record PerformanceParams(Resolution resolution, boolean unbufferedRendering) {

		public PerformanceParams (int pBufferSizeX, int pBufferSizeY, boolean unbufferedRendering) {
			this(new Resolution(pBufferSizeX, pBufferSizeY), unbufferedRendering);
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
			if (!unbufferedRendering) {
				bufferTarget = createJOGLTarget(drawable.getGL(), dataBbox, renderToTarget, config);
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

			AbstractJOGLOutput.clearGL(drawable.getGL(), clearColor);

			/* render to pBuffer */

			JOGLOutput target = (bufferTarget == null)?
					createJOGLTarget(drawable.getGL(), dataBbox, renderToTarget, config) : bufferTarget;

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
