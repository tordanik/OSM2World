package org.osm2world.map_elevation.creation;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.geo.MapProjection;

/**
 * strategy for elevation interpolation from a set of known points
 */
public interface TerrainInterpolator {

	/**
	 * Initializes the interpolator with the data to be used for interpolation.
	 * This is called exactly once before any calls to {@link #interpolateEle(VectorXZ)}.
	 *
	 * @param sites  non-empty dataset of points with known elevation
	 */
	void setKnownSites(TerrainEleData sites, MapProjection projection);

	VectorXYZ interpolateEle(VectorXZ pos);

}
