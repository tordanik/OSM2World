package org.osm2world.core.map_data.creation;

import static java.lang.Math.*;

import org.osm2world.core.math.VectorXZ;

/**
 * application of an orthographic projection that is intended to use values in meters centered around the coordinate
 * center (0,0). It projects coordinates onto a plane touching the globe at the origin.
 * This results in sufficient accuracy if the data covers only a "small" part of the globe.
 */
public class OrthographicAzimuthalMapProjection implements MapProjection {

	private final double GLOBE_RADIUS = 6371000;

	private final LatLon origin;
	private final double lat0;
	private final double lon0;

	public OrthographicAzimuthalMapProjection(LatLon origin) {

		this.origin = origin;

		this.lat0 = toRadians(getOrigin().lat);
		this.lon0 = toRadians(getOrigin().lon);

	}

	@Override
	public LatLon getOrigin() {
		return origin;
	}

	@Override
	public VectorXZ toXZ(double latDeg, double lonDeg) {

		if (origin == null) throw new IllegalStateException("the origin needs to be set first");

		double lat = toRadians(latDeg);
		double lon = toRadians(lonDeg);

		double x = GLOBE_RADIUS * cos(lat) * sin(lon - lon0);
		double y = GLOBE_RADIUS * (cos(lat0) * sin(lat) - sin(lat0) * cos(lat) * cos(lon - lon0));

		return new VectorXZ(x, y);

	}

	@Override
	public double toLat(VectorXZ pos) {

		if (origin == null) throw new IllegalStateException("the origin needs to be set first");

		double rho = sqrt(pos.x * pos.x + pos.z * pos.z);
		double c = asin(rho / GLOBE_RADIUS);

		if (rho > 0) {
			return toDegrees(asin( cos(c) * sin(lat0) + ( pos.z * sin(c) * cos(lat0) ) / rho ));
		} else {
			return toDegrees(lat0);
		}

	}

	@Override
	public double toLon(VectorXZ pos) {

		if (origin == null) throw new IllegalStateException("the origin needs to be set first");

		double rho = sqrt(pos.x * pos.x + pos.z * pos.z);
		double c = asin(rho / GLOBE_RADIUS);

		double div = rho * cos(lat0) * cos(c) - pos.z * sin(lat0) * sin(c);

		if (abs(div) > 1e-5) {
			return toDegrees(lon0 + atan2( pos.x * sin(c), div ));
		} else {
			return toDegrees(lon0);
		}

	}

}
