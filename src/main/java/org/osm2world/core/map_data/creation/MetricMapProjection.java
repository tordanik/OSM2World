package org.osm2world.core.map_data.creation;

import static org.osm2world.core.map_data.creation.MercatorProjection.*;

import org.osm2world.core.math.VectorXZ;

public class MetricMapProjection implements MapProjection {

	private final LatLon origin;
	private final double originX;
	private final double originY;
	private final double scaleFactor;

	public MetricMapProjection(LatLon origin) {

		this.origin = origin;

		this.scaleFactor = earthCircumference(origin.lat);
		this.originY = latToY(origin.lat) * scaleFactor;
		this.originX = lonToX(origin.lon) * scaleFactor;

	}

	@Override
	public LatLon getOrigin() {
		return origin;
	}

	@Override
	public VectorXZ toXZ(double lat, double lon) {

		if (origin == null) throw new IllegalStateException("the origin needs to be set first");

		double x = lonToX(lon) * scaleFactor - originX;
		double y = latToY(lat) * scaleFactor - originY;

		/* snap to mm precision, seems to reduce geometry exceptions */
		x = Math.round(x * 1000) / 1000.0d;
		y = Math.round(y * 1000) / 1000.0d;

		return new VectorXZ(x, y); // x and z(!) are 2d here

	}

	@Override
	public double toLat(VectorXZ pos) {

		if (origin == null) throw new IllegalStateException("the origin needs to be set first");

		return yToLat((pos.z + originY) / scaleFactor);

	}

	@Override
	public double toLon(VectorXZ pos) {

		if (origin == null) throw new IllegalStateException("the origin needs to be set first");

		return xToLon((pos.x + originX) / scaleFactor);

	}

}
