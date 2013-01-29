package org.osm2world.viewer.view.debug;

import org.osm2world.EleInterpolationStrategy;
import org.osm2world.NaturalNeighborStrategy;
import org.osm2world.viewer.model.RenderOptions;


public class NaturalNeighborStrategyDebugView extends InterpolationStrategyDebugView {

	public NaturalNeighborStrategyDebugView(RenderOptions renderOptions) {
		super(renderOptions);
	}
	
	@Override
	protected EleInterpolationStrategy buildStrategy() {
		return new NaturalNeighborStrategy();
	}
	
}
