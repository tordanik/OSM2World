package org.osm2world.core.map_data.creation;

import static org.osm2world.core.map_data.creation.MercatorProjection.*;

import org.osm2world.core.math.VectorXZ;

/**
 * Map projection that is intended to use the "dense" space
 * of floating point values by making all coordinates relative to
 * the origin. 1 meter distance is roughly represented by 1 internal unit.
 */
public class MetricMapProjection extends OriginMapProjection {
	
	private double originX;
	private double originY;
	private double scaleFactor;
		
	public VectorXZ calcPos(double lat, double lon) {
		double x = lonToX(lon) * scaleFactor - originX;
		double y = latToY(lat) * scaleFactor - originY;

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
		return yToLat((pos.z + originY) / scaleFactor);
	}

	@Override
	public double calcLon(VectorXZ pos) {
		return xToLon((pos.x + originX) / scaleFactor);
	}

	@Override
	public VectorXZ getNorthUnit() {
		return VectorXZ.Z_UNIT;
	}

	@Override
	public void setOrigin(LatLon origin) {
		super.setOrigin(origin);

		this.scaleFactor = earthCircumference(origin.lat);
		this.originY = latToY(origin.lat) * scaleFactor;
		this.originX = lonToX(origin.lon) * scaleFactor;
	}
}
