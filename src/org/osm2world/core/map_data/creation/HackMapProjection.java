package org.osm2world.core.map_data.creation;

import static org.osm2world.core.map_data.creation.MercatorProjection.*;

import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.osm.data.OSMNode;

/**
 * quick-and-dirty projection that is intended to use the "dense" space
 * of floating point values, and tries to make 1 meter the distance
 * represented by 1 internal unit
 */
public class HackMapProjection implements MapProjection {
	
	/**
	 * This is a correction value to make lonToX-values metric values
	 */
	private final static double LON_CORRECTION = 1.5;
	
	/**
	 * we remember the first lat/lon values so we can move all other
	 * lat/lon-values near (0,0)
	 */
	private final double firstLat, firstLon;
	
	public HackMapProjection(OSMData osmData) {
		if (osmData.getNodes().isEmpty()) {
			throw new IllegalArgumentException("OSM data must contain nodes");
		}
		OSMNode firstNode = osmData.getNodes().iterator().next();
		firstLat = firstNode.lat;
		firstLon = firstNode.lon;
	}

	public VectorXZ calcPos(double lat, double lon) {
		
		double x = lonToX(lon - firstLon) / LON_CORRECTION;
		double y = latToY(lat - firstLat);
		
		return new VectorXZ(x, y); //x and z(!) are 2d here
	}
	
	@Override
	public double calcLat(VectorXZ pos) {
		return yToLat(pos.z) + firstLat;
	}
	
	@Override
	public double calcLon(VectorXZ pos) {
		return xToLon(pos.x * LON_CORRECTION) + firstLon;
	}
	
	@Override
	public VectorXZ getNorthUnit() {
		return VectorXZ.Z_UNIT;
	}
	
}
