package org.osm2world.math.geo;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Math.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

/**
 * an area on the globe represented by two coordinate pairs,
 * each with latitude and longitude. Immutable.
 */
public class LatLonBounds implements GeoBounds {

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

	@Override
	public LatLonBounds latLonBounds() {
		return this;
	}

	@Override
	public String toString() {
		return String.format(Locale.ROOT, "(lat=%f..%f, lon=%f..%f)", minlat, maxlat, minlon, maxlon);
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

	public List<LatLon> getCorners() {
		return List.of(
				new LatLon(minlat, minlon),
				new LatLon(minlat, maxlon),
				new LatLon(maxlat, maxlon),
				new LatLon(maxlat, minlon));
	}

	public LatLon getCenter() {
		return new LatLon(minlat + sizeLat() / 2, minlon + sizeLon() / 2);
	}

	public boolean contains(LatLon p) {
		return p.lat >= minlat && p.lat <= maxlat && p.lon >= minlon && p.lon <= maxlon;
	}

	public boolean contains(LatLonBounds other) {
		return other.minlat >= this.minlat && other.maxlat <= this.maxlat
				&& other.minlon >= this.minlon && other.maxlon <= this.maxlon;
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


	/**
	 * Returns the intersection of a nonempty group of {@link LatLonBounds}.
	 * @return  null if the intersection is empty
	 */
	public static @Nullable LatLonBounds intersection(Collection<LatLonBounds> bounds) {

		double minlat = bounds.stream().mapToDouble(b -> b.minlat).max().orElseThrow(IllegalStateException::new);
		double maxlat = bounds.stream().mapToDouble(b -> b.maxlat).min().orElseThrow(IllegalStateException::new);
		double minlon = bounds.stream().mapToDouble(b -> b.minlon).max().orElseThrow(IllegalStateException::new);
		double maxlon = bounds.stream().mapToDouble(b -> b.maxlon).min().orElseThrow(IllegalStateException::new);

		if (maxlat <= minlat || maxlon <= minlon) {
			return null;
		} else {
			return new LatLonBounds(minlat, minlon, maxlat, maxlon);
		}

	}

	/** returns bounds which are a bit larger than this one */
	public LatLonBounds pad(double paddingSize) {

		if (paddingSize == 0) {
			return this;
		} else if (paddingSize > 0) {
			return new LatLonBounds(
					max(-90, minlat - paddingSize),
					max(-180, minlon - paddingSize),
					min(90, maxlat + paddingSize),
					min(180, maxlon + paddingSize));
		} else {
			if (abs(paddingSize) >= sizeLat() || abs(paddingSize) >= sizeLon()) {
				throw new IllegalArgumentException("attempting to shrink bounds by more than their size");
			}
			return new LatLonBounds(
					minlat - paddingSize,
					minlon - paddingSize,
					maxlat + paddingSize,
					maxlon + paddingSize);
		}

	}

	@Override
	public final boolean equals(Object o) {
		return o instanceof LatLonBounds that
				&& Double.compare(minlat, that.minlat) == 0
				&& Double.compare(minlon, that.minlon) == 0
				&& Double.compare(maxlat, that.maxlat) == 0
				&& Double.compare(maxlon, that.maxlon) == 0;
	}

	@Override
	public int hashCode() {
		int result = Double.hashCode(minlat);
		result = 31 * result + Double.hashCode(minlon);
		result = 31 * result + Double.hashCode(maxlat);
		result = 31 * result + Double.hashCode(maxlon);
		return result;
	}

}