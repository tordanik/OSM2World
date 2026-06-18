package org.osm2world.map_elevation.creation;

import java.util.Collection;

import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.LatLonEle;

/**
 * Terrain elevation data.
 * Usually obtained from a {@link TerrainEleDataSource}.
 * Contains the sites with known elevation within particular geographic bounds.
 */
public interface TerrainEleData {

	LatLonBounds bounds();

	Collection<LatLonEle> sites();

	default boolean isEmpty() { return sites().isEmpty(); }

}
