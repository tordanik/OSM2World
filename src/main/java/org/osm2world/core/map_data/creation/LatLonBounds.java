package org.osm2world.core.map_data.creation;

/**
 * an area on the globe represented by two coordinate pairs,
 * each with latitude and longitude. Immutable.
 */
public class LatLonBounds {

	public final double minlat;
	public final double minlon;
	public final double maxlat;
	public final double maxlon;

	public LatLonBounds(double minlat, double minlon, double maxlat, double maxlon) {
		this.minlat = minlat;
		this.minlon = minlon;
		this.maxlat = maxlat;
		this.maxlon = maxlon;
	}

	public LatLonBounds(LatLon min, LatLon max) {
		this.minlat = min.lat;
		this.minlon = min.lon;
		this.maxlat = max.lat;
		this.maxlon = max.lon;
	}

	public LatLon getMin() {
		return new LatLon(minlat, minlon);
	}

	public LatLon getMax() {
		return new LatLon(maxlat, maxlon);
	}

}