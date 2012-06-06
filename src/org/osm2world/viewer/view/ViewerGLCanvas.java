package org.osm2world.viewer.view;

import static javax.media.opengl.GL.*;
import static javax.media.opengl.GL2GL3.*;

import java.awt.Color;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;

import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.MessageManager;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.model.MessageManager.Message;
import org.osm2world.viewer.view.debug.DebugView;
import org.osm2world.viewer.view.debug.WorldObjectView;

import com.jogamp.opengl.util.FPSAnimator;

public class ViewerGLCanvas extends GLCanvas {

	public ViewerGLCanvas(Data data, MessageManager messageManager, RenderOptions renderOptions) {

		super(new GLCapabilities(GLProfile.getDefault()));
		
		setSize(800, 600);
		setIgnoreRepaint(true);

		addGLEventListener(new ViewerGLEventListener(data, messageManager, renderOptions));

		FPSAnimator animator = new FPSAnimator( this, 60 );
        
		animator.start();

	}
	

	public class ViewerGLEventListener implements GLEventListener {

		private final Data data;
		private final MessageManager messageManager;
		private final RenderOptions renderOptions;
		
		private final GLU glu = new GLU();
				
		public ViewerGLEventListener(Data data, MessageManager messageManager, RenderOptions renderOptions) {
			this.data = data;
			this.messageManager = messageManager;
			this.renderOptions = renderOptions;
		}

		@Override
		public void display(GLAutoDrawable glDrawable) {
			
	        final GL2 gl = glDrawable.getGL().getGL2();
	        
	        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	        
	        /* prepare projection matrix stack */
	       
	        if (renderOptions.camera != null) {
	        	
	        	//TODO: reactivate to allow
//		        //calculate height for orthographic projection to match
//		        //the height of the perspective view volume at lookAt's distance
//		        double dist = renderOptions.camera.getLookAt().subtract(
//		        		renderOptions.camera.getPos())
//		        		.length();
//		        double tanAngle = Math.tan(renderOptions.projection.getVertAngle());
//		        double height = tanAngle * dist;
//		        renderOptions.projection = renderOptions.projection.withVolumeHeight(height);
//
	        }
	        
	        JOGLTarget.setProjectionMatrices(gl, renderOptions.projection);
	        
	        /* prepare modelview matrix stack with camera information */
	        
	        gl.glLoadIdentity();
	        
	        if (renderOptions.camera != null) {
	        	JOGLTarget.setCameraMatrices(gl, renderOptions.camera);
	        }

	        /* choose wireframe or fill mode */
	        
	        if (renderOptions.isWireframe()) {
	        	gl.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
	        } else {
	        	gl.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
	        }
	        
	        /* enable or disable backface culling */
	        
	        if (renderOptions.isBackfaceCulling()) {
	        	gl.glEnable (GL.GL_CULL_FACE);
	        } else {
	        	gl.glDisable (GL.GL_CULL_FACE);
	        }

	        /* draw debug views */
	        
	        DebugView activeWorldObjectView = null;
	        
	        for (DebugView debugView : renderOptions.activeDebugViews) {
	        	if (debugView instanceof WorldObjectView) {
	        		// needs to be rendered last because of transparency
	        		activeWorldObjectView = debugView;
	        		continue;
	        	}
	        	debugView.renderTo(gl, renderOptions.camera, renderOptions.projection);
	        }
	        
	        if (activeWorldObjectView != null) {
	        	activeWorldObjectView.renderTo(gl, renderOptions.camera, renderOptions.projection);
	        }
	        
	        /* write messages */
	        
	        int messageCount = 0;
	        for (Message message : messageManager.getLiveMessages()) {
	        	new JOGLTarget(gl, renderOptions.camera).drawText(message.messageString,
	        			10, 10 + messageCount * 20,
	        			ViewerGLCanvas.this.getWidth(),
	        			ViewerGLCanvas.this.getHeight(),
	        			Color.WHITE);
	        	messageCount++;
	        }
	        
			gl.glFlush();

		}

		@Override
		public void init(GLAutoDrawable glDrawable) {
			final GL gl = glDrawable.getGL();
					    		
			gl.glFrontFace(GL.GL_CCW);                  // use ccw polygons
			
	        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);    // Black Background
	        gl.glEnable (GL.GL_DEPTH_TEST);             // z buffer
			gl.glCullFace(GL.GL_BACK);
	        
		}

		@Override
		public void reshape(GLAutoDrawable gLDrawable,
				int x, int y, int width, int height) {
			
			final GL gl = gLDrawable.getGL();

	        if (height <= 0) { // avoid a divide by zero error!
	            height = 1;
	        }
	        	        
	        gl.glViewport(0, 0, width, height);
	        
	        renderOptions.projection =
	        	renderOptions.projection.withAspectRatio((double)width / height);
	        
		}
	
		@Override
		public void dispose(GLAutoDrawable arg0) {
			
		}
		
	}

}
