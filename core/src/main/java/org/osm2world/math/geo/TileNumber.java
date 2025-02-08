package org.osm2world.math.geo;

import static java.lang.Math.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

/**
 * immutable tile number with zoom level.
 * Tile coords follow the common XYZ convention, with an Y axis that points southward.
 */
public class TileNumber implements TileBounds {

	public final int zoom;
	public final int x;
	public final int y;

	/**
	 * pattern for parsing constructor
	 */
	public static final String PATTERN = "([0-9]{1,2})[,/_]([0-9]{1,9})[,/_]([0-9]{1,9})";

	/**
	 * regular constructor
	 *
	 * @throws IllegalArgumentException  for invalid tile numbers
	 */
	public TileNumber(int zoom, int x, int y) {
		this.zoom = zoom;
		this.x = x;
		this.y = y;
		validateValues();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + zoom;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TileNumber other = (TileNumber) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		if (zoom != other.zoom)
			return false;
		return true;
	}

	/**
	 * parsing constructor
	 * @param arg  string to be parsed; format see {@link #PATTERN}
	 * @throws IllegalArgumentException  for invalid tile numbers
	 */
	public TileNumber(@Nonnull String arg) {
		Matcher m = Pattern.compile(PATTERN).matcher(arg);
		if (m.matches()) {
			zoom = Integer.parseInt(m.group(1));
			x = Integer.parseInt(m.group(2));
			y = Integer.parseInt(m.group(3));
			validateValues();
		} else {
			throw new IllegalArgumentException("argument doesn't match: " + arg);
		}
	}

	/**
	 * @throws IllegalArgumentException  for incorrect field values
	 */
	private void validateValues() {
		if (zoom < 0) {
			throw new IllegalArgumentException("illegal tile number, zoom must not be negative: " + toString());
		} else if (x < 0 || y < 0) {
			throw new IllegalArgumentException("illegal tile number, x and y must not be negative: " + toString());
		} else if (x >= (1 << zoom)) {
			throw new IllegalArgumentException("illegal tile number, x too large: " + toString());
		} else if (y >= (1 << zoom)) {
			throw new IllegalArgumentException("illegal tile number, y too large: " + toString());
		}
	}

	/** returns a flipped y coordinate for use with TMS tile coords (with an Y axis pointing northward) */
	public int flippedY() {
		return (1 << zoom) - 1 - y;
	}

	/** formats this tile number as a string that matches {@link #PATTERN} */
	@Override
	public String toString() {
		return toString(",");
	}

	/** variant of {@link #toString()} with a custom separator between the components of the tile number */
	public String toString(String separator) {
		return zoom + separator + x + separator + y;
	}

	@Override
	public Set<TileNumber> getTiles() {
		return Set.of(this);
	}

	public LatLonBounds latLonBounds() {
		LatLon min = new LatLon(tile2lat(y + 1, zoom), tile2lon(x, zoom));
		LatLon max = new LatLon(tile2lat(y, zoom), tile2lon(x + 1, zoom));
		return new LatLonBounds(min, max);
	}

	/** returns the {@link TileNumber} at the given zoom and location */
	public static TileNumber atLatLon(int zoom, LatLon latLon) {
		int x = (int)floor((latLon.lon + 180) / 360 * (1<<zoom));
		int y = (int)floor((1 - log(tan(toRadians(latLon.lat)) + 1 / cos(toRadians(latLon.lat))) / PI) / 2 * (1<<zoom));
		return new TileNumber(zoom, x, y);
	}

	/** returns the {@link TileNumber}s of all tiles overlapping the given bounds */
	public static List<TileNumber> tilesForBounds(int zoom, LatLonBounds bounds) {

		TileNumber min = atLatLon(zoom, bounds.getMin());
		TileNumber max = atLatLon(zoom, bounds.getMax());

		List<TileNumber> result = new ArrayList<TileNumber>();

		for (int x = min.x; x <= max.x; x++) {
			for (int y = max.y; y <= min.y; y++) {
				result.add(new TileNumber(zoom, x, y));
			}
		}

		return result;

	}

	static final double tile2lon(int x, int z) {
		return x / pow(2.0, z) * 360.0 - 180;
	}

	static final double tile2lat(int y, int z) {
		double n = PI - (2.0 * PI * y) / pow(2.0, z);
		return toDegrees(atan(sinh(n)));
	}

}
