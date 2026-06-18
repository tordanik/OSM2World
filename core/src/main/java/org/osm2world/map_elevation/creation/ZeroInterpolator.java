package org.osm2world.map_elevation.creation;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.geo.MapProjection;

/**
 * sets every point's elevation to 0
 */
public class ZeroInterpolator implements TerrainInterpolator {

	public void setKnownSites(TerrainEleData eleData, MapProjection projection) {
		// do nothing
	}

	@Override
	public VectorXYZ interpolateEle(VectorXZ pos) {
		return pos.xyz(0);
	}

}
