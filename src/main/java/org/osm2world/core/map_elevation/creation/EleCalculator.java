package org.osm2world.core.map_elevation.creation;

import javax.annotation.Nonnull;

import org.osm2world.core.map_data.data.MapData;

/**
 * calculates elevations using terrain elevation and information from {@link MapData}
 */
public interface
EleCalculator {

	/**
	 * provides elevation information for all elements in the {@link MapData}.
	 * The result will be provided to the {@link org.osm2world.core.map_elevation.data.EleConnector}s
	 * in the map data. It is assumed that connectors' elevations are initially set to the elevation of the terrain.
	 */
	void calculateElevations(@Nonnull MapData mapData);

}
