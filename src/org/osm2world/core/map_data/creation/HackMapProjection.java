package org.osm2world.core.map_data.creation;

import static org.osm2world.core.map_data.creation.MercatorProjection.*;

import org.osm2world.core.math.VectorXZ;

/**
 * quick-and-dirty projection that is intended to use the "dense" space
 * of floating point values, and tries to make 1 meter the distance
 * represented by 1 internal unit
 */
public class HackMapProjection extends OriginMapProjection {
	
	private double originX;
	private double originY;
	private double groundScale;
	
	/*
	 * All coordinates will be modified by subtracting the origin
	 * (in lat/lon, which does not really make sense, but is simply
	 *  supposed to keep nodes as close as possible to 0.0).
	 * 
	 * //TODO: replace this solution later
	 */
	
	public VectorXZ calcPos(double lat, double lon) {
		double x = lonToX(lon) * groundScale - originX;
		double y = latToY(lat) * groundScale - originY;

		/* snap to som cm precision, seems to reduce geometry exceptions */
		x = Math.round(x * 1000) / 1000.0d;
		y = Math.round(y * 1000) / 1000.0d;

		return new VectorXZ(x, y); // x and z(!) are 2d here
	}

	@Override
	public VectorXZ calcPos(LatLon latlon) {
		return calcPos(latlon.lat, latlon.lon);
	}

	@Override
	public double calcLat(VectorXZ pos) {
		return yToLat((pos.z + originY) / groundScale);
	}

	@Override
	public double calcLon(VectorXZ pos) {
		return xToLon((pos.x + originX) / groundScale);
	}

	@Override
	public VectorXZ getNorthUnit() {
		return VectorXZ.Z_UNIT;
	}

	@Override
	public void setOrigin(LatLon origin) {
		super.setOrigin(origin);

		this.groundScale = MercatorProjection.earthCircumference(origin.lat);
		this.originY = latToY(origin.lat) * groundScale;
		this.originX = lonToX(origin.lon) * groundScale;

		System.out.println(origin.lat + " " + origin.lon + " / " + originX
				+ ":" + originY + " scale:" + groundScale);
	}
}
