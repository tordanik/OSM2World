package org.osm2world.viewer.view.debug;

import org.osm2world.TerrainInterpolator;
import org.osm2world.NaturalNeighborInterpolator;
import org.osm2world.viewer.model.RenderOptions;


public class NaturalNeighborStrategyDebugView extends InterpolationStrategyDebugView {

	public NaturalNeighborStrategyDebugView(RenderOptions renderOptions) {
		super(renderOptions);
	}
	
	@Override
	protected TerrainInterpolator buildStrategy() {
		return new NaturalNeighborInterpolator();
	}
	
}
