package org.osm2world.map_elevation.creation;

import java.util.Collection;

import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.LatLonEle;

/**
 * Terrain elevation data which consists of an unstructured collection of sites with known elevation.
 */
public record TerrainEleDataCollection(
		LatLonBounds bounds,
		Collection<LatLonEle> sites
) implements TerrainEleData {}
