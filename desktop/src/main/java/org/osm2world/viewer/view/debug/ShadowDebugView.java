package org.osm2world.viewer.view.debug;

import static org.osm2world.output.jogl.JOGLRenderingParameters.Winding.CCW;

import org.osm2world.output.common.lighting.GlobalLightingParameters;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.output.jogl.JOGLOutputShader;
import org.osm2world.output.jogl.JOGLRenderingParameters;
import org.osm2world.viewer.model.RenderOptions;

public class ShadowDebugView extends DebugView {

	private final RenderOptions renderOptions;

	public ShadowDebugView(RenderOptions renderOptions) {
		super("Shadow debug view", "shows the world from the perspective of the light");
		this.renderOptions = renderOptions;
	}

	@Override
	protected void fillTarget(JOGLOutput output) {
		if (output instanceof JOGLOutputShader joglOutput) {
			setParameters(joglOutput);
			joglOutput.setXZBoundary(scene.getBoundary());
			joglOutput.setShowShadowPerspective(true);
			joglOutput.outputScene(scene, true);
		}
	}

	@Override
	protected void updateTarget(JOGLOutput target, boolean viewChanged) {
		setParameters(target);
	}

	private void setParameters(final JOGLOutput target) {

		boolean drawBoundingBox = config.getBoolean("drawBoundingBox", false);
		boolean shadowVolumes = "shadowVolumes".equals(config.getString("shadowImplementation"))
				|| "both".equals(config.getString("shadowImplementation"));
		boolean shadowMaps = "shadowMap".equals(config.getString("shadowImplementation"))
				|| "both".equals(config.getString("shadowImplementation"));
		int shadowMapWidth = config.getInt("shadowMapWidth", 4096);
		int shadowMapHeight = config.getInt("shadowMapHeight", 4096);
		int shadowMapCameraFrustumPadding = config.getInt("shadowMapCameraFrustumPadding", 8);
		target.setRenderingParameters(new JOGLRenderingParameters(
				renderOptions.isBackfaceCulling() ? CCW : null,
    			renderOptions.isWireframe(), true, drawBoundingBox, shadowVolumes, shadowMaps, shadowMapWidth, shadowMapHeight,
    			shadowMapCameraFrustumPadding, false, 0, 0, false));

		target.setGlobalLightingParameters(GlobalLightingParameters.DEFAULT);

	}

}
