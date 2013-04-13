package org.osm2world.viewer.view.debug;

import org.osm2world.TerrainInterpolator;
import org.osm2world.LeastSquaresInterpolator;
import org.osm2world.viewer.model.RenderOptions;


public class LeastSquaresStrategyDebugView extends InterpolationStrategyDebugView {

	public LeastSquaresStrategyDebugView(RenderOptions renderOptions) {
		super(renderOptions);
	}
	
	@Override
	protected TerrainInterpolator buildStrategy() {
		return new LeastSquaresInterpolator();
	}
	
}
