package org.osm2world.viewer.view.debug;

import org.osm2world.core.target.common.lighting.GlobalLightingParameters;
import org.osm2world.core.target.jogl.JOGLTarget;

public class TerrainView extends DebugView {
	
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
		target.setGlobalLightingParameters(GlobalLightingParameters.DEFAULT);
		terrain.renderTo(target);
	}

}
