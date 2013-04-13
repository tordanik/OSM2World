package org.osm2world.viewer.view.debug;

import org.osm2world.TerrainInterpolator;
import org.osm2world.InverseDistanceWeightingInterpolator;
import org.osm2world.viewer.model.RenderOptions;


public class InverseDistanceWeightingStrategyDebugView extends InterpolationStrategyDebugView {
	
	public InverseDistanceWeightingStrategyDebugView(RenderOptions renderOptions) {
		super(renderOptions);
	}
	
	@Override
	protected TerrainInterpolator buildStrategy() {
		return new InverseDistanceWeightingInterpolator(1);
	}
	
}
