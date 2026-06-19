package org.osm2world.math.geo;

import static java.lang.Math.*;

import java.util.Objects;

/**
 * an immutable coordinate pair with latitude and longitude
 */
public class LatLon {

	/** latitude in degrees */
	public final double lat;

	/** longitude in degrees */
	public final double lon;

	// typographical minus '−' works around the CLI parser's special handling of '-'
	public static final String DOUBLE_PATTERN = "[+-−]?\\d+(?:\\.\\d+)?";

	/** pattern for parseable arguments */
	public static final String PATTERN = "("+DOUBLE_PATTERN+"),("+DOUBLE_PATTERN+")";

	public LatLon(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
		validateValues();
	}

	/** parsing constructor for strings matching {@link #PATTERN} */
	public LatLon(String string) {
		LatLonEle lle = new LatLonEle(string);
		this.lat = lle.lat;
		this.lon = lle.lon;
		validateValues();
	}

	/**
	 * @throws IllegalArgumentException  for incorrect field values
	 */
	private void validateValues() {
		if (lat > 90 || lat < -90 || lon > 180 || lon < -180) {
			throw new IllegalArgumentException("Latitude or longitude not valid: " + lat + ", " + lon);
		}
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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		LatLon latLon = (LatLon) o;
		return Double.compare(latLon.lat, lat) == 0 && Double.compare(latLon.lon, lon) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(lat, lon);
	}

	@Override
	public String toString() {
		return "(" + lat + ", " + lon + ")";
	}

}
