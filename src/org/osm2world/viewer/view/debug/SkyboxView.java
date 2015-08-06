package org.osm2world.viewer.view.debug;

import static java.lang.Math.sqrt;

import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.jogl.JOGLRenderingParameters;
import org.osm2world.core.target.jogl.JOGLTarget;

public class SkyboxView extends DebugView {
	
	@Override
	public String getDescription() {
		return "shows a skybox in the background";
	};
	
	@Override
	public boolean canBeUsed() {
		return map != null;
	}
	
	@Override
	protected void fillTarget(JOGLTarget target) {
		
		target.setGlobalLightingParameters(GlobalLightingParameters.DEFAULT);
		
		// disable backface culling
		target.setRenderingParameters(
				new JOGLRenderingParameters(null, false, true, false, false));
		
		// draw the skybox close to the limits of the viewing distance
		double skyboxSize = 1.95 * projection.getFarClippingDistance() / sqrt(3);
		
		target.drawBox(Materials.SKYBOX,
				camera.getPos().add(0, -skyboxSize / 2, 0),
				VectorXZ.Z_UNIT, skyboxSize, skyboxSize, skyboxSize);
		
		target.finish();
		
	}

	@Override
	protected void updateTarget(JOGLTarget target, boolean viewChanged) {
		if (viewChanged) {
			target.reset();
			fillTarget(target);
		}
	}
	
}
