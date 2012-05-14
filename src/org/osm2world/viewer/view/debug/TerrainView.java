package org.osm2world.viewer.view.debug;

import javax.media.opengl.GL;

import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.primitivebuffer.JOGLPrimitiveBufferRenderer;
import org.osm2world.core.target.primitivebuffer.PrimitiveBuffer;

public class TerrainView extends DebugView {
	
	@Override
	public String getDescription() {
		return "shows the terrain";
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
	protected void renderToImpl(GL gl, Camera camera) {
		
		if (renderer == null) {
			renderer = new JOGLPrimitiveBufferRenderer(gl, terrainPrimitiveBuffer);
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
		
		renderer.render(camera);

		// switch lighting off
		
		gl.glDisable(GL.GL_LIGHT0);
		gl.glDisable(GL.GL_LIGHTING);
		
	}

}
