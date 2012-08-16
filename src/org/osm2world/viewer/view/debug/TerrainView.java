package org.osm2world.viewer.view.debug;

import static org.osm2world.core.target.jogl.JOGLRenderingParameters.Winding.CCW;

import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.jogl.JOGLRenderingParameters;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.viewer.model.RenderOptions;

public class TerrainView extends DebugView {
	
	private final RenderOptions renderOptions;
	
	public TerrainView(RenderOptions renderOptions) {
		this.renderOptions = renderOptions;
	}
	
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
		setParameters(target);
		terrain.renderTo(target);
	}
	
	@Override
	protected void updateTarget(JOGLTarget target, boolean viewChanged) {
		setParameters(target);
	}
	
	private void setParameters(JOGLTarget target) {
		
		target.setRenderingParameters(new JOGLRenderingParameters(
				renderOptions.isBackfaceCulling() ? CCW : null,
				renderOptions.isWireframe(), true));
		
		target.setGlobalLightingParameters(GlobalLightingParameters.DEFAULT);
		
	}
	
}
