package org.osm2world;

import java.io.IOException;
import java.util.Collection;

import org.osm2world.core.math.VectorXYZ;


public interface TerrainElevationData {

	Collection<VectorXYZ> getSites(double minLon, double minLat,
			double maxLon, double maxLat) throws IOException;
	
}
