package org.osm2world.viewer.view.debug;

import static org.osm2world.output.jogl.JOGLRenderingParameters.Winding.CCW;

import org.osm2world.output.common.lighting.GlobalLightingParameters;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.output.jogl.JOGLRenderingParameters;
import org.osm2world.viewer.model.RenderOptions;

public class WorldObjectView extends DebugView {

	private final RenderOptions renderOptions;

	public WorldObjectView(RenderOptions renderOptions) {
		super("World objects", "shows the world objects");
		this.renderOptions = renderOptions;
	}

	@Override
	protected void fillTarget(JOGLOutput output) {

		setParameters(output);
		output.setXZBoundary(scene.getBoundary());
		output.outputScene(scene, true);

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
		boolean useSSAO = "true".equals(config.getString("useSSAO"));
		int SSAOkernelSize = config.getInt("SSAOkernelSize", 16);
		float SSAOradius = config.getFloat("SSAOradius", 1);
		boolean overwriteProjectionClippingPlanes = "true".equals(config.getString("overwriteProjectionClippingPlanes"));
		target.setRenderingParameters(new JOGLRenderingParameters(
				renderOptions.isBackfaceCulling() ? CCW : null,
    			renderOptions.isWireframe(), true, drawBoundingBox, shadowVolumes, shadowMaps, shadowMapWidth, shadowMapHeight,
    			shadowMapCameraFrustumPadding, useSSAO, SSAOkernelSize, SSAOradius, overwriteProjectionClippingPlanes));

		target.setGlobalLightingParameters(GlobalLightingParameters.DEFAULT);

	}

}
