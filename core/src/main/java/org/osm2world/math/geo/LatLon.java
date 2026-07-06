package org.osm2world.math.geo;

import static java.lang.Math.*;

import javax.annotation.Nonnull;

/**
 * immutable coordinate pair with latitude and longitude
 *
 * @param lat latitude in degrees
 * @param lon longitude in degrees
 */
public record LatLon(double lat, double lon) {

	// typographical minus '−' works around the CLI parser's special handling of '-'
	public static final String DOUBLE_PATTERN = "[+-−]?\\d+(?:\\.\\d+)?";

	/** pattern for parseable arguments */
	public static final String PATTERN = "(" + DOUBLE_PATTERN + "),(" + DOUBLE_PATTERN + ")";

	public LatLon {
		if (lat > 90 || lat < -90 || lon > 180 || lon < -180) {
			throw new IllegalArgumentException("Latitude or longitude not valid: " + lat + ", " + lon);
		}
	}

	/** parsing constructor for strings matching {@link #PATTERN} */
	public LatLon(String string) {
		this(new LatLonEle(string).lat(), new LatLonEle(string).lon());
	}

	public static LatLon LonLat(double lon, double lat) {
		return new LatLon(lat, lon);
	}

	/** returns the approximate distance to another coordinate in meters */
	public double distanceTo(LatLon pos) {
		/* Haversine formula. Assumes spherical earth, does not consider elevation. */
		final double earthRadius = 6371 * 1000.0;
		double latDistance = toRadians(pos.lat - this.lat);
		double lonDistance = toRadians(pos.lon - this.lon);
		double a = sin(latDistance / 2) * sin(latDistance / 2)
				+ cos(toRadians(this.lat)) * cos(toRadians(pos.lat))
				* sin(lonDistance / 2) * sin(lonDistance / 2);
		double c = 2 * atan2(sqrt(a), sqrt(1 - a));
		return earthRadius * c;
	}

	@Override
	public @Nonnull String toString() {
		return "(" + lat + ", " + lon + ")";
	}

}
