package org.osm2world.map_elevation.creation;

import java.io.IOException;
import java.util.Collection;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;

/**
 * a source of terrain elevation data. Implementations may range from raster
 * data such as SRTM to sparsely distributed points with known elevation.
 */
public interface TerrainElevationData {

	/**
	 * returns all points with known elevation within the bounds
	 */
	Collection<VectorXYZ> getSites(AxisAlignedRectangleXZ bounds) throws IOException;

}
