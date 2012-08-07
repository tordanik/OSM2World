package org.osm2world.viewer.view.debug;

import static javax.media.opengl.fixedfunc.GLLightingFunc.*;
import static org.osm2world.core.util.FaultTolerantIterationUtil.iterate;

import javax.media.opengl.GL2;

import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.util.FaultTolerantIterationUtil.Operation;
import org.osm2world.core.world.data.WorldObject;

public class WorldObjectView extends DebugView {
	
	@Override
	public String getDescription() {
		return "shows the world objects";
	};

	@Override
	public boolean canBeUsed() {
		return map != null;
	}
	
	@Override
	protected void fillTarget(final JOGLTarget target) {
		
		iterate(map.getWorldObjects(), new Operation<WorldObject>() {
			@Override public void perform(WorldObject w) {
				TargetUtil.renderObject(target, w);
			}
		});
		
	}
	
	@Override
	protected void renderToImpl(GL2 gl, Camera camera, Projection projection) {
				
		// define light source
		
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
