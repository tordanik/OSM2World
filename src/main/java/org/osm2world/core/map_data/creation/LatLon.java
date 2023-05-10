package org.osm2world.core.map_data.creation;

import java.util.Objects;

import org.osm2world.console.LatLonEle;

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
