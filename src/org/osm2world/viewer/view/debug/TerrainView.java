package org.osm2world.viewer.view.debug;

import static javax.media.opengl.fixedfunc.GLLightingFunc.*;

import javax.media.opengl.GL2;

import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
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
	protected void renderToImpl(GL2 gl, Camera camera, Projection projection) {
		
		if (renderer == null) {
			renderer = new JOGLPrimitiveBufferRenderer(gl, terrainPrimitiveBuffer);
		}

		// define light source
		
		gl.glLightfv(GL_LIGHT0, GL_AMBIENT,
				new float[] {1.0f, 1.0f, 1.0f , 1.0f}, 0);
		gl.glLightfv(GL_LIGHT0, GL_DIFFUSE,
				new float[] {1.0f, 1.0f, 1.0f , 1.0f}, 0);
		gl.glLightfv(GL_LIGHT0, GL_SPECULAR,
				new float[] {1.0f, 1.0f, 1.0f , 1.0f}, 0);
		gl.glLightfv(GL_LIGHT0, GL_POSITION,
				new float[] {1.0f, 1.5f, -(-1.0f), 0.0f}, 0);
		
		gl.glEnable(GL_LIGHT0);
		gl.glEnable(GL_LIGHTING);
		
		// render
		
		if (camera != null && projection != null) {
			renderer.render(camera, projection);
		}

		// switch lighting off
		
		gl.glDisable(GL_LIGHT0);
		gl.glDisable(GL_LIGHTING);
		
	}

}
