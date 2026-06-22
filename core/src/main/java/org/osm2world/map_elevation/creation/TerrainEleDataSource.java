package org.osm2world.map_elevation.creation;

import java.io.IOException;

import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;

/**
 * a source of terrain elevation data. Implementations may range from raster
 * data such as SRTM to sparsely distributed points with known elevation.
 */
public interface TerrainEleDataSource {

	/**
	 * returns all points with known elevation within the bounds
	 */
	TerrainEleData getSites(LatLonBounds bounds) throws IOException;

	/**
	 * returns all points with known elevation within the bounds,
	 * projected to the local coordinate system
	 */
	default TerrainEleData getSites(AxisAlignedRectangleXZ bounds, MapProjection projection) throws IOException {

		var bufferedBounds = bounds.pad(100);

		var latLonBounds = new LatLonBounds(
				projection.toLatLon(bufferedBounds.bottomLeft()),
				projection.toLatLon(bufferedBounds.topRight()));

		return getSites(latLonBounds);

	}

}
