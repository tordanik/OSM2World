package org.osm2world.viewer.view.debug;

import static org.osm2world.core.target.jogl.JOGLRenderingParameters.Winding.CCW;

import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.jogl.JOGLRenderingParameters;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.target.jogl.JOGLTargetShader;
import org.osm2world.viewer.model.RenderOptions;

public class ShadowView extends DebugView {
	
	private final RenderOptions renderOptions;
	
	public ShadowView(RenderOptions renderOptions) {
		this.renderOptions = renderOptions;
	}
	
	@Override
	public String getDescription() {
		return "shows the world from the perspective of the light";
	};
	
	@Override
	public boolean canBeUsed() {
		return map != null;
	}
	
	@Override
	protected void fillTarget(final JOGLTarget target) {
		if (target instanceof JOGLTargetShader) {
			setParameters(target);
			target.setXZBoundary(map.getBoundary());
			((JOGLTargetShader)target).setShowShadowPerspective(true);
		
			boolean underground = config.getBoolean("renderUnderground", true);
		
			TargetUtil.renderWorldObjects(target, map, underground);
		}
	}
	
	@Override
	protected void updateTarget(JOGLTarget target, boolean viewChanged) {
		setParameters(target);
	}
	
	private void setParameters(final JOGLTarget target) {
		
		boolean shadowVolumes = "shadowVolumes".equals(config.getString("shadowImplementation"))
				|| "both".equals(config.getString("shadowImplementation"));
		boolean shadowMaps = "shadowMap".equals(config.getString("shadowImplementation"))
				|| "both".equals(config.getString("shadowImplementation"));
		target.setRenderingParameters(new JOGLRenderingParameters(
				renderOptions.isBackfaceCulling() ? CCW : null,
    			renderOptions.isWireframe(), true, shadowVolumes, shadowMaps));
		
		target.setGlobalLightingParameters(GlobalLightingParameters.DEFAULT);
		
	}

}
