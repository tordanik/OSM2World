package org.osm2world;

import java.util.Collection;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

/**
 * sets every point's elevation to 0
 */
public class ZeroInterpolator implements TerrainInterpolator {
	
	@Override
	public void setKnownSites(Collection<VectorXYZ> sites) {
		// do nothing
	}
	
	@Override
	public VectorXYZ interpolateEle(VectorXZ pos) {
		return pos.xyz(0);
	}
	
}
