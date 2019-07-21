package org.osm2world.core.map_elevation.creation;

import java.io.IOException;
import java.util.Collection;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.math.VectorXYZ;

/**
 * a source of terrain elevation data. Implementations may range from raster
 * data such as SRTM to sparsely distributed points with known elevation.
 */
public interface TerrainElevationData {

	Collection<VectorXYZ> getSites(double minLon, double minLat,
			double maxLon, double maxLat) throws IOException;

	Collection<VectorXYZ> getSites(MapData mapData) throws IOException;

}
