package org.osm2world.viewer.view.debug;

import org.osm2world.core.map_elevation.creation.NaturalNeighborInterpolator;
import org.osm2world.viewer.model.RenderOptions;


public class NaturalNeighborInterpolatorDebugView extends TerrainInterpolatorDebugView {

	public NaturalNeighborInterpolatorDebugView(RenderOptions renderOptions) {
		super(renderOptions);
	}

	@Override
	protected NaturalNeighborInterpolator buildInterpolator() {
		return new NaturalNeighborInterpolator();
	}

}
