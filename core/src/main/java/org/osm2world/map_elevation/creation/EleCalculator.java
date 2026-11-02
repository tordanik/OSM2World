package org.osm2world.map_elevation.creation;

import javax.annotation.Nonnull;

import org.osm2world.map_data.data.MapData;
import org.osm2world.world.connectors.Connector;

/**
 * calculates elevations using terrain elevation and information from {@link MapData}
 */
public interface EleCalculator {

	/**
	 * provides elevation information for all elements in the {@link MapData}.
	 * The result will be provided to the {@link Connector}s in the map data.
	 * <p>
	 * This is called after connectors have been attached to surfaces. Attached connectors must not be moved.
	 * The terrain elevation at each connector is available from {@link Connector#getTerrainEle()}.
	 * Calling this method multiple times must produce the same result.
	 *
	 * @param terrain  provides the terrain elevation at arbitrary positions
	 */
	void calculateElevations(@Nonnull MapData mapData, @Nonnull TerrainInterpolator terrain);

}
