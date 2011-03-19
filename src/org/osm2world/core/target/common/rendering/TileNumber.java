package org.osm2world.core.target.common.rendering;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * immutable tile number with zoom level
 */
public class TileNumber {

	public final int zoom;
	public final int x;
	public final int y;

	/**
	 * pattern for parsing constructor
	 */
	public static final String PATTERN = "([0-9]{1,2}),([0-9]{1,9}),([0-9]{1,9})";
	
	/**
	 * regular constructor
	 */
	public TileNumber(int zoom, int x, int y) {
		this.zoom = zoom;
		this.x = x;
		this.y = y;
		validateValues();
	}

	/**
	 * parsing constructor
	 * @param arg  string to be parsed;
	 *             format see {@link #PATTERN}
	 */
	public TileNumber(String arg) {
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
		if (zoom <= 0 || x <= 0 || y <= 0) {
			//TODO (robustness): more validation
			throw new IllegalArgumentException("not positive: " + x + ", " + y);
		}
	}
	
	@Override
	public String toString() {		
		return zoom + "," + x + "," + y;
	}
	
}
