package org.osm2world.viewer.view;

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
import org.osm2world.viewer.model.MessageManager.Message;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.debug.DebugView;
import org.osm2world.viewer.view.debug.HelpView;
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
				
		private final HelpView helpView = new HelpView();
		
		public ViewerGLEventListener(Data data, MessageManager messageManager, RenderOptions renderOptions) {
			this.data = data;
			this.messageManager = messageManager;
			this.renderOptions = renderOptions;
		}

		@Override
		public void display(GLAutoDrawable glDrawable) {
			
	        final GL2 gl = glDrawable.getGL().getGL2();
	        
	        JOGLTarget.clearGL(gl, Color.BLACK);
	        
	        helpView.renderTo(gl, null, null);
	        
	        if (renderOptions.camera != null) {
	        	
	        	/* prepare projection matrix stack */
	        	
	        	//TODO: reactivate to allow
//		        //calculate height for orthographic projection to match
//		        //the height of the perspective view volume at lookAt's distance
//		        double dist = renderOptions.camera.getLookAt().subtract(
//		        		renderOptions.camera.getPos())
//		        		.length();
//		        double tanAngle = Math.tan(renderOptions.projection.getVertAngle());
//		        double height = tanAngle * dist;
//		        renderOptions.projection = renderOptions.projection.withVolumeHeight(height);
	        		        	
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
	        		JOGLTarget.drawText(message.messageString,
	        				10, 10 + messageCount * 20,
	        				ViewerGLCanvas.this.getWidth(),
	        				ViewerGLCanvas.this.getHeight(),
	        				Color.WHITE);
	        		messageCount++;
	        	}
	        	
	        	gl.glFlush();

	        }
			
		}

		@Override
		public void init(GLAutoDrawable glDrawable) {
			//initialization is performed within JOGLTarget
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
