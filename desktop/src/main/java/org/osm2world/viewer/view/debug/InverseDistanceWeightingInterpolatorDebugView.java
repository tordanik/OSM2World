package org.osm2world.viewer.view.debug;

import org.osm2world.map_elevation.creation.InverseDistanceWeightingInterpolator;
import org.osm2world.viewer.model.RenderOptions;


public class InverseDistanceWeightingInterpolatorDebugView extends TerrainInterpolatorDebugView {

	public InverseDistanceWeightingInterpolatorDebugView(RenderOptions renderOptions) {
		super(renderOptions, "InverseDistanceWeightingInterpolator");
	}

	@Override
	protected InverseDistanceWeightingInterpolator buildInterpolator() {
		return new InverseDistanceWeightingInterpolator(1);
	}

}
