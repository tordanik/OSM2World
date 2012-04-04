package org.osm2world.viewer.view.debug;

import javax.media.opengl.GL;

import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.primitivebuffer.JOGLPrimitiveBufferRenderer;
import org.osm2world.core.target.primitivebuffer.PrimitiveBuffer;

public class WorldObjectView extends DebugView {
	
	@Override
	public String getDescription() {
		return "shows the world objects";
	};
	
	JOGLPrimitiveBufferRenderer renderer = null;
		
	@Override
	public void setPrimitiveBuffers(PrimitiveBuffer gridPrimitiveBuffer,
			PrimitiveBuffer terrainPrimitiveBuffer) {
		
		super.setPrimitiveBuffers(gridPrimitiveBuffer, terrainPrimitiveBuffer);
		
		if (renderer != null) {
			renderer.freeResources();
			renderer = null;
		}
		
	}

	@Override
	public boolean canBeUsed() {
		return map != null;
	}
	
	@Override
	protected void renderToImpl(GL gl, Camera camera) {

		if (renderer == null) {
			renderer = new JOGLPrimitiveBufferRenderer(gl, mapDataPrimitiveBuffer);
		}
		
		// define light source
		
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
		
		// render
		
		renderer.render();
		
		// switch lighting off
		
		gl.glDisable(GL.GL_LIGHT0);
		gl.glDisable(GL.GL_LIGHTING);
		
	}

}
