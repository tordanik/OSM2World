package org.osm2world.console;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;

import org.apache.commons.configuration.Configuration;
import org.osm2world.console.CLIArgumentsUtil.OutputMode;
import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.target.primitivebuffer.JOGLPrimitiveBufferRenderer;
import org.osm2world.core.target.primitivebuffer.PrimitiveBuffer;

import com.sun.opengl.util.Screenshot;

public final class ImageExport {

	/**
	 * the width and height of the canvas used for rendering the exported image
	 * each must not exceed the canvas limit. If the requested image is larger,
	 * it will be rendered in multiple passes and combined afterwards.
	 * This is intended to avoid overwhelmingly large canvases
	 * (which would lead to crashes)
	 */
	private static final int DEFAULT_CANVAS_LIMIT = 1024;

	private static final String BG_COLOR_CONFIG_KEY = "backgroundColor";
	private static final String CANVAS_LIMIT_CONFIG_KEY = "canvasLimit";
	
	private ImageExport() { }
	
	/**
	 * 
	 * @param outputFile
	 * @param outputMode   one of the image output modes
	 * @param x            horizontal resolution
	 * @param y            vertical resolution
	 * @param results
	 * @param camera
	 * @param projection
	 * @throws IOException
	 */
	public static void writeImageFile(
			Configuration config,
			File outputFile, OutputMode outputMode,
			int x, int y,
			final Results results, final Camera camera,
			final Projection projection) throws IOException {
		
		if (! GLDrawableFactory.getFactory().canCreateGLPbuffer()) {
			throw new Error("Cannot create GLPbuffer for OpenGL output!");
		}
		
		/* parse background color and other configuration options */
		
		float[] clearColor = {0f, 0f, 0f};
		
		if (config.containsKey(BG_COLOR_CONFIG_KEY)) {
			try {
				Color.decode(config.getString(BG_COLOR_CONFIG_KEY))
					.getColorComponents(clearColor);
			} catch (NumberFormatException e) {
				System.err.println("incorrect color value: "
						+ config.getString(BG_COLOR_CONFIG_KEY));
			}
		}
		
		int canvasLimit = config.getInt(CANVAS_LIMIT_CONFIG_KEY, DEFAULT_CANVAS_LIMIT);
		
		/* determine the number of "parts" to split the rendering in */
		
		int xParts = 1 + ((x-1) / canvasLimit);
		int yParts = 1 + ((y-1) / canvasLimit);
		
		/* create GL canvas and set rendering parameters */
		
		final GL gl;
		
		GLCapabilities cap = new GLCapabilities();
		cap.setDoubleBuffered(false);
		
		int pBufferX = xParts > 1 ? canvasLimit : x;
		int pBufferY = yParts > 1 ? canvasLimit : y;
		
		GLPbuffer pBuffer = GLDrawableFactory.getFactory().createGLPbuffer(
				cap, null, pBufferX, pBufferY, null);
		
		gl = pBuffer.getGL();
		pBuffer.getContext().makeCurrent();
		
		gl.glFrontFace(GL.GL_CCW);                  // use ccw polygons
					
		gl.glClearColor(clearColor[0], clearColor[1], clearColor[2], 1.0f);
        gl.glEnable (GL.GL_DEPTH_TEST);             // z buffer
		gl.glCullFace(GL.GL_BACK);
        gl.glEnable (GL.GL_CULL_FACE);              // backface culling
       

		gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT,
				new float[] {1.0f, 1.0f, 1.0f , 1.0f}, 0);
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE,
				new float[] {1.0f, 1.0f, 1.0f , 1.0f}, 0);
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR,
				new float[] {1.0f, 1.0f, 1.0f , 1.0f}, 0);
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION,
				new float[] {1.0f, 1.5f, -(-1.0f), 0.0f}, 0);
		
		gl.glEnable(GL.GL_LIGHT0);
		gl.glEnable(GL.GL_LIGHTING);
				
        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
        
        
		/* render map data into buffer if it needs to be rendered multiple times */
		
		JOGLPrimitiveBufferRenderer bufferRenderer = null;
		
		if ((xParts > 1 || yParts > 1)
				&& !config.getBoolean("forceUnbufferedPNGRendering", false)) {
			
			PrimitiveBuffer buffer = new PrimitiveBuffer();
			
			TargetUtil.renderWorldObjects(buffer, results.getMapData());
			TargetUtil.renderObject(buffer, results.getTerrain());
			
			bufferRenderer = new JOGLPrimitiveBufferRenderer(gl, buffer);
			
		}
					
		/* create image (maybe in multiple parts) */
				
        BufferedImage image = new BufferedImage(x, y, BufferedImage.TYPE_INT_RGB);
        		
		for (int xPart = 0; xPart < xParts; ++xPart) {
		for (int yPart = 0; yPart < yParts; ++yPart) {
			
			/* calculate start, end and size (in pixels)
			 * of the image part that will be rendered in this pass */
			
			int xStart = xPart * canvasLimit;
			int xEnd   = (xPart+1 < xParts) ? (xStart + (canvasLimit-1)) : (x-1);
			int xSize  = (xEnd - xStart) + 1;
			
			int yStart = yPart * canvasLimit;
			int yEnd   = (yPart+1 < yParts) ? (yStart + (canvasLimit-1)) : (y-1);
			int ySize  = (yEnd - yStart) + 1;
			
			/* configure rendering */
	        
	        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
	        
	        gl.glLoadIdentity();
	        
	        JOGLTarget.setProjectionMatricesForPart(gl, projection,
	        		xStart / (double)(x-1), xEnd / (double)(x-1),
	        		yStart / (double)(y-1), yEnd / (double)(y-1));
	        
	       	JOGLTarget.setCameraMatrices(gl, camera);
	             
	        
	        /* render to pBuffer */
	        
	        if (bufferRenderer != null) {
	        	
	        	bufferRenderer.render();

	        } else {
	        	
		        JOGLTarget jogl = new JOGLTarget(gl, camera);
		        
				TargetUtil.renderWorldObjects(jogl, results.getMapData());
				TargetUtil.renderObject(jogl, results.getTerrain());
				
			}
	        
	        /* make screenshot and paste into the buffer
	         * that will contain the entire image*/

	        BufferedImage imagePart =
	        	Screenshot.readToBufferedImage(pBufferX, pBufferY);
	        
	        image.getGraphics().drawImage(imagePart,
	        		xStart, y-1-yEnd, xSize, ySize, null);
	        			
		}
		}
		
        /* clean up */
		
		bufferRenderer.freeResources();
		bufferRenderer = null;
		
        pBuffer.getContext().release();
        pBuffer.destroy();
        pBuffer = null;
		
		/* write the entire image */
        
		switch (outputMode) {
			
			case PNG: ImageIO.write(image, "png", outputFile); break;
			case PPM: writePPMFile(image, outputFile); break;
			
			default: throw new IllegalArgumentException(
					"output mode not supported " + outputMode);
			
		}
		
	}

	private static void writePPMFile(BufferedImage image, File outputFile)
			throws IOException {
				
		FileOutputStream out = null;
		FileChannel fc = null;
		
		try {
			
			out = new FileOutputStream(outputFile);
			
			// write header
			
			Charset charSet = Charset.forName("US-ASCII");
			out.write("P6\n".getBytes(charSet));
			out.write(String.format("%d %d\n", image.getWidth(), image.getHeight())
					.getBytes(charSet));
			out.write("255\n".getBytes(charSet));
						
			// collect and write content

			ByteBuffer writeBuffer = ByteBuffer.allocate(
					3 * image.getWidth() * image.getHeight());
			
			DataBuffer imageDataBuffer = image.getRaster().getDataBuffer();
			int[] data = (((DataBufferInt)imageDataBuffer).getData());
			
			for (int value : data) {
				writeBuffer.put((byte)(value >>> 16));
				writeBuffer.put((byte)(value >>> 8));
				writeBuffer.put((byte)(value));
			}
			
			writeBuffer.position(0);
			
			fc = out.getChannel();
			fc.write(writeBuffer);
			
		} finally {
			
			if (fc != null) {
				fc.close();
			} else if(out != null) {
				out.close();
			}
			
		}

	}
	
}
