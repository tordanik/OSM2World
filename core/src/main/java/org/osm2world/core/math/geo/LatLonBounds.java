package org.osm2world.core.math.geo;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;

import java.util.ArrayList;
import java.util.List;

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
		this(min.lat, min.lon, max.lat, max.lon);
	}

	public double sizeLat() {
		return maxlat - minlat;
	}

	public double sizeLon() {
		return maxlon - minlon;
	}

	public LatLon getMin() {
		return new LatLon(minlat, minlon);
	}

	public LatLon getMax() {
		return new LatLon(maxlat, maxlon);
	}

	public LatLon getCenter() {
		return new LatLon(minlat + sizeLat() / 2, minlon + sizeLon() / 2);
	}

	public static LatLonBounds ofPoints(Iterable<LatLon> points) {

		double minLat = POSITIVE_INFINITY;
		double maxLat = NEGATIVE_INFINITY;
		double minLon = POSITIVE_INFINITY;
		double maxLon = NEGATIVE_INFINITY;

		for (LatLon p : points) {
			if (p.lat < minLat) {
				minLat = p.lat;
			}
			if (p.lat > maxLat) {
				maxLat = p.lat;
			}
			if (p.lon < minLon) {
				minLon = p.lon;
			}
			if (p.lon > maxLon) {
				maxLon = p.lon;
			}
		}

		return new LatLonBounds(minLat, minLon, maxLat, maxLon);

	}

	/** returns the union of a nonempty group of {@link LatLonBounds} */
	public static LatLonBounds union(Iterable<LatLonBounds> bounds) {

		List<LatLon> points = new ArrayList<>();

		for (LatLonBounds b : bounds) {
			points.add(b.getMin());
			points.add(b.getMax());
		}

		if (points.isEmpty()) {
			throw new IllegalArgumentException("parameter must not be empty");
		}

		return LatLonBounds.ofPoints(points);

	}

}