package org.osm2world.viewer.view.debug;

import org.osm2world.EleInterpolationStrategy;
import org.osm2world.InverseDistanceWeightingStrategy;
import org.osm2world.viewer.model.RenderOptions;


public class InverseDistanceWeightingStrategyDebugView extends InterpolationStrategyDebugView {
	
	public InverseDistanceWeightingStrategyDebugView(RenderOptions renderOptions) {
		super(renderOptions);
	}
	
	@Override
	protected EleInterpolationStrategy buildStrategy() {
		return new InverseDistanceWeightingStrategy(1);
	}
	
}
