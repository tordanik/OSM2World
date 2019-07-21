package org.osm2world.core.heightmap.data;

import java.util.Collection;

/**
 * elevation data for the ground.
 * Rather abstract interface that doesn't impose restrictions
 * on the order of points with known elevation.
 */
public interface TerrainElevation {

	public Collection<TerrainPoint> getTerrainPoints();

}
