package org.osm2world.viewer.view.debug;

import org.osm2world.map_elevation.creation.LeastSquaresInterpolator;
import org.osm2world.viewer.model.RenderOptions;


public class LeastSquaresInterpolatorDebugView extends TerrainInterpolatorDebugView {

	public LeastSquaresInterpolatorDebugView(RenderOptions renderOptions) {
		super(renderOptions, "LeastSquaresInterpolator");
	}

	@Override
	protected LeastSquaresInterpolator buildInterpolator() {
		return new LeastSquaresInterpolator();
	}

}
