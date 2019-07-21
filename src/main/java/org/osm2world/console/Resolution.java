package org.osm2world.console;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * immutable representation of an image resolution
 * (two positive integers)
 */
public class Resolution {

	public final int x;
	public final int y;

	/**
	 * pattern for parseable arguments
	 */
	public static final String PATTERN = "([0-9]{1,9}),([0-9]{1,9})";

	/**
	 * regular constructor
	 */
	public Resolution(int x, int y) {
		this.x = x;
		this.y = y;
		validateValues();
	}

	/**
	 * parsing constructor
	 * @param arg  command line argument to be parsed;
	 *             format see {@link #PATTERN}
	 */
	public Resolution(String arg) {
		Matcher m = Pattern.compile(PATTERN).matcher(arg);
		if (m.matches()) {
			x = Integer.parseInt(m.group(1));
			y = Integer.parseInt(m.group(2));
			validateValues();
		} else {
			throw new IllegalArgumentException("argument doesn't match: " + arg);
		}
	}

	/**
	 * @throws IllegalArgumentException  for incorrect field values
	 */
	private void validateValues() {
		if (x <= 0 || y <= 0) {
			throw new IllegalArgumentException("not positive: " + x + ", " + y);
		}
	}

	@Override
	public String toString() {
		return x + "," + y;
	}

}
