package org.osm2world.core.map_data.creation;

/**
 * an immutable coordinate pair with latitude and longitude
 */
public class LatLon {

	public final double lat;
	public final double lon;

	public LatLon(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}

	@Override
	public String toString() {
		return "(" + lat + ", " + lon + ")";
	}

}
