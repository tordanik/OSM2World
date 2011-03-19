package org.osm2world.viewer.view.debug;

import javax.media.opengl.GL;

import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.jogl.RenderableToJOGL;


public final class DebugModels {

	private DebugModels() { }
	
	public static class CoordinateAxes implements RenderableToJOGL {
				
		private final float centerX, centerY, centerZ;
		private final float lineLength;
				
		public CoordinateAxes(float centerX, float centerY, float centerZ, float lineLength) {
			this.centerX = centerX;
			this.centerY = centerY;
			this.centerZ = centerZ;
			this.lineLength = lineLength;
		}

		@Override
		public void renderTo(GL gl, Camera camera) {
			
			//x axis
			gl.glColor3f(1, 0, 0);			
	        gl.glBegin(GL.GL_LINE);
	        gl.glVertex3f(centerX, centerY, centerZ);
	        gl.glVertex3f(centerX+lineLength, centerY, centerZ);
	        gl.glEnd();

			//y axis
			gl.glColor3f(0, 1, 0);			
	        gl.glBegin(GL.GL_LINE);
	        gl.glVertex3f(centerX, centerY, centerZ);
	        gl.glVertex3f(centerX, centerY+lineLength, centerZ);
	        gl.glEnd();

			//z axis
			gl.glColor3f(0, 0, 1);			
	        gl.glBegin(GL.GL_LINE);
	        gl.glVertex3f(centerX, centerY, centerZ);
	        gl.glVertex3f(centerX, centerY, centerZ+lineLength);
	        gl.glEnd();
	        
		}
	}
	
}
