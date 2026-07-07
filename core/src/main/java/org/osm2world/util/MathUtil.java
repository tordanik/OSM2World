package org.osm2world.util;

public class MathUtil {

	/**
	 * Clamps a value to a given range.
	 * TODO: replace with standard library equivalent after upgrade to Java 21+
	 */
	public static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(value, max));
	}

}
