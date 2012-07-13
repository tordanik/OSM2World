package org.osm2world.viewer.view.debug;

import static javax.media.opengl.fixedfunc.GLLightingFunc.*;

import javax.media.opengl.GL2;

import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.jogl.JOGLTarget;
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

		JOGLTarget.setLightingParameters(gl, GlobalLightingParameters.DEFAULT);
				
		// render
		
		if (camera != null && projection != null) {
			renderer.render(camera, projection);
		}

		// switch lighting off
		
		gl.glDisable(GL_LIGHT0);
		gl.glDisable(GL_LIGHTING);
		
	}

}
