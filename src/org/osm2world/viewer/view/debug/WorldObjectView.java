package org.osm2world.viewer.view.debug;

import static org.osm2world.core.target.jogl.JOGLRenderingParameters.Winding.CCW;

import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.jogl.JOGLRenderingParameters;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.viewer.model.RenderOptions;

public class WorldObjectView extends DebugView {
	
	private final RenderOptions renderOptions;
	
	public WorldObjectView(RenderOptions renderOptions) {
		this.renderOptions = renderOptions;
	}
	
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
		
		setParameters(target);
		target.setXZBoundary(map.getBoundary());
		
		boolean underground = config.getBoolean("renderUnderground", true);
		
		TargetUtil.renderWorldObjects(target, map, underground);
		
	}
	
	@Override
	protected void updateTarget(JOGLTarget target, boolean viewChanged) {
		setParameters(target);
	}
	
	private void setParameters(final JOGLTarget target) {
		
		target.setRenderingParameters(new JOGLRenderingParameters(
				renderOptions.isBackfaceCulling() ? CCW : null,
    			renderOptions.isWireframe(), true));
		
		target.setGlobalLightingParameters(GlobalLightingParameters.DEFAULT);
		
	}

}
