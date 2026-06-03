package org.osm2world.map_elevation.creation;

import java.io.IOException;
import java.util.Collection;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.LatLonEle;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;

/**
 * a source of terrain elevation data. Implementations may range from raster
 * data such as SRTM to sparsely distributed points with known elevation.
 */
public interface TerrainElevationData {

	/**
	 * returns all points with known elevation within the bounds
	 */
	Collection<LatLonEle> getSites(LatLonBounds bounds) throws IOException;

	/**
	 * returns all points with known elevation within the bounds,
	 * projected to the local coordinate system
	 */
	default Collection<VectorXYZ> getSites(AxisAlignedRectangleXZ bounds, MapProjection projection) throws IOException {

		var latLonBounds = new LatLonBounds(
				projection.toLatLon(bounds.bottomLeft()),
				projection.toLatLon(bounds.topRight()));

		var bufferedBounds = latLonBounds.pad(0.005);

		return getSites(bufferedBounds).stream()
				.map(p -> projection.toXZ(p.lat, p.lon).xyz(p.ele))
				.toList();

	}

}
