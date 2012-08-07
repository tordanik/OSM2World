package org.osm2world.viewer.view.debug;

import static javax.media.opengl.fixedfunc.GLLightingFunc.*;

import javax.media.opengl.GL2;

import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.jogl.JOGLTarget;

public class TerrainView extends DebugView {
	
	@Override
	public String getDescription() {
		return "shows the terrain";
	};
	
	@Override
	public boolean canBeUsed() {
		return terrain != null;
	}

	@Override
	protected void fillTarget(JOGLTarget target) {
		terrain.renderTo(target);
	}
	
	@Override
	protected void renderToImpl(GL2 gl, Camera camera, Projection projection) {
		
		JOGLTarget.setLightingParameters(gl, GlobalLightingParameters.DEFAULT);
				
		// render
		
		if (camera != null && projection != null) {
			target.render(camera, projection);
		}

		// switch lighting off
		
		gl.glDisable(GL_LIGHT0);
		gl.glDisable(GL_LIGHTING);
		
	}

}
