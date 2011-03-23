package org.osm2world.console;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.swing.JFrame;

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
	 * the canvas used for rendering the exported image must be at most
	 * CANVAS_LIMIT wide and high. If the requested image is larger,
	 * it will be rendered in multiple passes and combined afterwards.
	 * This is intended to avoid overwhelmingly large canvases
	 * (which would lead to crashes)
	 */
	private static final int CANVAS_LIMIT = 1024;
	
	private ImageExport() { }
	
	/**
	 * 
	 * @param outputFile
	 * @param x            horizontal resolution
	 * @param y            vertical resolution
	 * @param results
	 * @param camera
	 * @param projection
	 * @throws IOException
	 */
	public static void writeImageFile(
			File outputFile, int x, int y,
			final Results results, final Camera camera,
			final Projection projection) throws IOException {
		
		/* render map data into buffer */
		
		PrimitiveBuffer buffer = new PrimitiveBuffer();
		
		TargetUtil.renderWorldObjects(buffer, results.getMapData());
		TargetUtil.renderObject(buffer, results.getTerrain());
		
		/* create image (maybe in multiple parts) */
				
        BufferedImage image = new BufferedImage(x, y, BufferedImage.TYPE_INT_RGB);
        		
		int xParts = 1 + ((x-1) / CANVAS_LIMIT);
		int yParts = 1 + ((y-1) / CANVAS_LIMIT);
		
		for (int xPart = 0; xPart < xParts; ++xPart) {
		for (int yPart = 0; yPart < yParts; ++yPart) {
			
			/* calculate start, end and size (in pixels)
			 * of the image part that will be rendered in this pass */
			
			int xStart = xPart * CANVAS_LIMIT;
			int xEnd   = (xPart+1 < xParts) ? (xStart + (CANVAS_LIMIT-1)) : (x-1);
			int xSize  = (xEnd - xStart) + 1;
			
			int yStart = yPart * CANVAS_LIMIT;
			int yEnd   = (yPart+1 < yParts) ? (yStart + (CANVAS_LIMIT-1)) : (y-1);
			int ySize  = (yEnd - yStart) + 1;
						
			/* create and configure canvas */
			
			GLCanvas canvas = new GLCanvas(new GLCapabilities());
			canvas.setSize(xSize, ySize);
			        
			JFrame frame = new JFrame();
			frame.add(canvas);
			frame.pack();
			frame.setVisible(true); // TODO remove this line
			
			final GL gl = canvas.getGL();
			canvas.getContext().makeCurrent();
	        
			gl.glFrontFace(GL.GL_CCW);                  // use ccw polygons
			
	        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);    // Black Background
	        gl.glEnable (GL.GL_DEPTH_TEST);             // z buffer
			gl.glCullFace(GL.GL_BACK);
	        gl.glEnable (GL.GL_CULL_FACE);              // backface culling
	        			
	        
	        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
	               
	        JOGLTarget.setProjectionMatricesForPart(gl, projection,
	        		xStart / (double)(x-1), xEnd / (double)(x-1),
	        		yStart / (double)(y-1), yEnd / (double)(y-1));
	        
	       	JOGLTarget.setCameraMatrices(gl, camera);
	        
	        
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
	        
	        /* render to canvas */
	        
	        new JOGLPrimitiveBufferRenderer(gl, buffer).render();
	        
	        //TODO add from buffer
	        
	        /* make screenshot and paste into the buffer
	         * that will contain the entire image*/

	        BufferedImage imagePart =
	        	Screenshot.readToBufferedImage(xSize, ySize);
	        	        
	        image.getGraphics().drawImage(imagePart, xStart, y-1-yEnd, null);
	        
	        /* clean up */
			
			canvas.getContext().release();
			frame.dispose();
			
		}
		}
		
		/* write the entire image */
        
        ImageIO.write(image, "png", outputFile);
		
	}
	
}
