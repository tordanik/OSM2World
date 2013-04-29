package org.osm2world.viewer.view.debug;

import org.osm2world.LinearInterpolator;
import org.osm2world.viewer.model.RenderOptions;

public class LinearInterpolatorDebugView extends TerrainInterpolatorDebugView {
	
	public LinearInterpolatorDebugView(RenderOptions renderOptions) {
		super(renderOptions);
	}
	
	@Override
	protected LinearInterpolator buildInterpolator() {
		return new LinearInterpolator();
	}
	
}
