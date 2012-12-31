package org.osm2world.viewer.view.debug;

import org.osm2world.EleInterpolationStrategy;
import org.osm2world.LeastSquaresStrategy;
import org.osm2world.viewer.model.RenderOptions;


public class LeastSquaresStrategyDebugView extends InterpolationStrategyDebugView {

	public LeastSquaresStrategyDebugView(RenderOptions renderOptions) {
		super(renderOptions);
	}
	
	@Override
	protected EleInterpolationStrategy buildStrategy() {
		return new LeastSquaresStrategy();
	}
	
}
