package org.osm2world.core.map_elevation.creation;

import java.util.Collection;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

/**
 * strategy for elevation interpolation from a set of known points
 */
public interface TerrainInterpolator {

	/**
	 * @param sites  non-empty collection of points with known elevation
	 */
	void setKnownSites(Collection<VectorXYZ> sites);

	VectorXYZ interpolateEle(VectorXZ pos);

}
