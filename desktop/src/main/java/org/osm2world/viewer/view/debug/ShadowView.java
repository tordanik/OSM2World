package org.osm2world.viewer.view.debug;

import static org.osm2world.output.jogl.JOGLRenderingParameters.Winding.CCW;

import org.osm2world.output.OutputUtil;
import org.osm2world.output.common.lighting.GlobalLightingParameters;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.output.jogl.JOGLOutputShader;
import org.osm2world.output.jogl.JOGLRenderingParameters;
import org.osm2world.viewer.model.RenderOptions;

public class ShadowView extends DebugView {

	private final RenderOptions renderOptions;

	public ShadowView(RenderOptions renderOptions) {
		this.renderOptions = renderOptions;
	}

	@Override
	public String getDescription() {
		return "shows the world from the perspective of the light";
	}

	@Override
	public boolean canBeUsed() {
		return map != null;
	}

	@Override
	protected void fillTarget(final JOGLOutput target) {
		if (target instanceof JOGLOutputShader) {
			setParameters(target);
			target.setXZBoundary(map.getBoundary());
			((JOGLOutputShader)target).setShowShadowPerspective(true);

			boolean underground = config.getBoolean("renderUnderground", true);

			OutputUtil.renderWorldObjects(target, map, underground);
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
