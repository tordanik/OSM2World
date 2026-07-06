package org.osm2world.viewer.view.debug;

import org.osm2world.map_elevation.creation.LocalCellInterpolator;
import org.osm2world.viewer.model.RenderOptions;

public class LocalCellInterpolatorDebugView extends TerrainInterpolatorDebugView {

	public LocalCellInterpolatorDebugView(RenderOptions renderOptions) {
		super(renderOptions, "LocalCellInterpolator");
	}

	@Override
	protected LocalCellInterpolator buildInterpolator() {
		return new LocalCellInterpolator();
	}

}
