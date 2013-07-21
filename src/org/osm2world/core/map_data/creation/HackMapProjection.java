package org.osm2world.core.map_data.creation;

import static org.osm2world.core.map_data.creation.MercatorProjection.*;

import org.osm2world.core.math.VectorXZ;

/**
 * quick-and-dirty projection that is intended to use the "dense" space
 * of floating point values, and tries to make 1 meter the distance
 * represented by 1 internal unit
 */
public class HackMapProjection extends OriginMapProjection {
	
	/**
	 * This is a correction value to make lonToX-values metric values
	 */
	private final static double LON_CORRECTION = 1.5;
	
	/*
	 * All coordinates will be modified by subtracting the origin
	 * (in lat/lon, which does not really make sense, but is simply
	 *  supposed to keep nodes as close as possible to 0.0).
	 * 
	 * //TODO: replace this solution later
	 */
	
	public VectorXZ calcPos(double lat, double lon) {
		
		double x = lonToX(lon - origin.lon) / LON_CORRECTION;
		double y = latToY(lat - origin.lat);
		
		return new VectorXZ(x, y); //x and z(!) are 2d here
	}
	
	@Override
	public VectorXZ calcPos(LatLon latlon) {
		return calcPos(latlon.lat, latlon.lon);
	}
	
	@Override
	public double calcLat(VectorXZ pos) {
		return yToLat(pos.z) + origin.lat;
	}
	
	@Override
	public double calcLon(VectorXZ pos) {
		return xToLon(pos.x * LON_CORRECTION) + origin.lon;
	}
	
	@Override
	public VectorXZ getNorthUnit() {
		return VectorXZ.Z_UNIT;
	}
	
}
