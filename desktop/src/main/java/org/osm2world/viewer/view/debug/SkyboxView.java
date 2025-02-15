package org.osm2world.viewer.view.debug;

import static java.lang.Math.sqrt;

import org.osm2world.math.VectorXZ;
import org.osm2world.output.common.lighting.GlobalLightingParameters;
import org.osm2world.output.common.material.Materials;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.output.jogl.JOGLRenderingParameters;

public class SkyboxView extends DebugView {

	@Override
	public String getDescription() {
		return "shows a skybox in the background";
	}

	@Override
	public boolean canBeUsed() {
		return scene != null;
	}

	@Override
	protected void fillTarget(JOGLOutput target) {

		target.setGlobalLightingParameters(GlobalLightingParameters.DEFAULT);

		// disable backface culling
		target.setRenderingParameters(
				new JOGLRenderingParameters(null, false, true));

		// draw the skybox close to the limits of the viewing distance
		double skyboxSize = 1.95 * projection.farClippingDistance() / sqrt(3);

		target.drawBox(Materials.SKYBOX,
				camera.pos().add(0, -skyboxSize / 2, 0),
				VectorXZ.Z_UNIT, skyboxSize, skyboxSize, skyboxSize);

		target.finish();

	}

	@Override
	protected void updateTarget(JOGLOutput target, boolean viewChanged) {
		if (viewChanged) {
			target.reset();
			fillTarget(target);
		}
	}

}
