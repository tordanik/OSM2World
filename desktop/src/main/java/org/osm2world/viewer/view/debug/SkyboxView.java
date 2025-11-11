package org.osm2world.viewer.view.debug;

import static java.lang.Math.sqrt;

import org.osm2world.math.VectorXZ;
import org.osm2world.output.common.lighting.GlobalLightingParameters;
import org.osm2world.output.common.rendering.Camera;
import org.osm2world.output.common.rendering.Projection;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.output.jogl.JOGLRenderingParameters;
import org.osm2world.scene.material.Materials;

public class SkyboxView extends DebugView {

	public SkyboxView() {
		super("Skybox", "shows a skybox in the background");
	}

	@Override
	public boolean canBeUsed() {
		return scene != null && Materials.SKYBOX.get().getNumTextureLayers() > 0;
	}

	@Override
	protected void updateOutput(JOGLOutput output, boolean viewChanged, Camera camera, Projection projection) {

		if (viewChanged) {
			return;
		} else if (!output.isFinished()) {
			output.reset();
		}

		output.setGlobalLightingParameters(GlobalLightingParameters.DEFAULT);

		// disable backface culling
		output.setRenderingParameters(
				new JOGLRenderingParameters(null, false, true));

		// draw the skybox close to the limits of the viewing distance
		double skyboxSize = 1.95 * projection.farClippingDistance() / sqrt(3);

		output.drawBox(Materials.SKYBOX,
				camera.pos().add(0, -skyboxSize / 2, 0),
				VectorXZ.Z_UNIT, skyboxSize, skyboxSize, skyboxSize);

		output.finish();

	}

}
