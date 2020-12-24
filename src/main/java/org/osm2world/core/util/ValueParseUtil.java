package org.osm2world.core.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/** parses the syntax of typical OSM tag values */
public final class ValueParseUtil {

	/** prevents instantiation */
	private ValueParseUtil() { }

	/** pattern that splits into a part before and after a decimal point */
	private static final Pattern DEC_POINT_PATTERN = Pattern.compile("^(\\-?\\d+)\\.(\\d+)$");

	/**
	 * parses a non-negative integer value (e.g. "15", "0" or "910")
	 *
	 * @return  the parsed value as an integer; null if value is null, negative or has syntax errors.
	 */
	public static final Integer parseUInt(@Nullable String value) {
		if (value == null) return null;
		try {
			int result = Integer.parseInt(value);
			if (result >= 0) {
				return result;
			} else {
				return null;
			}
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** variant of {@link #parseUInt(String)} with a default value */
	public static final int parseUInt(@Nullable String value, int defaultValue) {
		Integer result = parseUInt(value);
		return result == null ? defaultValue : result;
	}

	/**
	 * parses an integer value (e.g. "15", "0", "-77" or "910")
	 *
	 * @return  the parsed value as an integer; null if value is null, negative or has syntax errors.
	 */
	public static final Integer parseInt(@Nullable String value) {
		if (value == null) return null;
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** variant of {@link #parseInt(String)} with a default value */
	public static final int parseInt(@Nullable String value, int defaultValue) {
		Integer result = parseInt(value);
		return result == null ? defaultValue : result;
	}

	/**
	 * parses a decimal value (e.g. "5", "0", "3.56" or "-12.30")
	 *
	 * @return  the parsed value as a floating point number; null if value is null or has syntax errors.
	 */
	public static final @Nullable Double parseOsmDecimal(@Nullable String value, boolean allowNegative) {

		if (value == null) return null;

		/* positive integer */

		try {

			int weight = Integer.parseInt(value);
			if (weight >= 0 || allowNegative) {
				return (double)weight;
			}

		} catch (NumberFormatException nfe) {}

		/* positive number with decimal point */

		Matcher matcher = DEC_POINT_PATTERN.matcher(value);

		if (matcher.matches()) {

			String stringBeforePoint = matcher.group(1);
			String stringAfterPoint = matcher.group(2);

			if (stringBeforePoint.length() > 0 || stringAfterPoint.length() > 0) {

				try {

					boolean negative = stringBeforePoint.startsWith("-");

					double beforePoint = Integer.parseInt(stringBeforePoint);
					double afterPoint = Integer.parseInt(stringAfterPoint);

					double result = Math.abs(beforePoint)
							+ Math.pow(10, -stringAfterPoint.length()) * afterPoint;
					if (negative) { result = - result; }

					if (result >= 0 || allowNegative) {
						return result;
					}

				} catch (NumberFormatException nfe) {}

			}
		}

		return null;
	}

	/** variant of {@link #parseOsmDecimal(String, boolean)} with a default value */
	public static final double parseOsmDecimal(@Nullable String value, boolean allowNegative, double defaultValue) {
		Double result = parseOsmDecimal(value, allowNegative);
		return result == null ? defaultValue : result;
	}


	private static final Pattern KMH_PATTERN = Pattern.compile("^(\\d+)\\s*km/h$");
	private static final Pattern MPH_PATTERN = Pattern.compile("^(\\d+)\\s*mph$");

	private static final double KM_PER_MILE = 1.609344f;

	/**
	 * parses a speed value given e.g. for the "maxspeed" key.
	 *
	 * @return  speed in km/h; null if value is null or has syntax errors.
	 */
	public static final @Nullable Double parseSpeed(@Nullable String value) {

		if (value == null) return null;

		/* try numeric speed (implied km/h) */

		Double speed = parseOsmDecimal(value, false);
		if (speed != null) {
			return speed;
		}

		/* try km/h speed */

		Matcher kmhMatcher = KMH_PATTERN.matcher(value);
		if (kmhMatcher.matches()) {
			String kmhString = kmhMatcher.group(1);
			try {
				return (double)Integer.parseInt(kmhString);
			} catch (NumberFormatException nfe) {}
		}

		/* try mph speed */

		Matcher mphMatcher = MPH_PATTERN.matcher(value);
		if (mphMatcher.matches()) {
			String mphString = mphMatcher.group(1);
			try {
				int mph = Integer.parseInt(mphString);
				return KM_PER_MILE * mph;
			} catch (NumberFormatException nfe) {}
		}

		/* all possibilities failed */

		return null;
	}

	/** variant of {@link #parseSpeed(String)} with a default value */
	public static final double parseSpeed(@Nullable String value, double defaultValue) {
		Double result = parseSpeed(value);
		return result == null ? defaultValue : result;
	}

	private static final Pattern M_PATTERN = Pattern.compile("^([\\d\\.]+)\\s*m$");
	private static final Pattern KM_PATTERN = Pattern.compile("^([\\d\\.]+)\\s*km$");
	private static final Pattern MI_PATTERN = Pattern.compile("^([\\d\\.]+)\\s*mi$");
	private static final Pattern FEET_INCHES_PATTERN = Pattern.compile("^([\\d]+)'\\s*([\\d]+)\"");

	private static final double M_PER_MI = 1609.344;
	private static final double M_PER_INCH = 0.0254f;

	/**
	 * parses a measure value given e.g. for the "width" or "length" key.
	 *
	 * @return  measure in m; null if value is null or has syntax errors.
	 */
	public static final @Nullable Double parseMeasure(@Nullable String value) {

		if (value == null) return null;

		/* try numeric measure (implied m) */

		Double measure = parseOsmDecimal(value, false);
		if (measure != null) {
			return measure;
		}

		/* try m measure */

		Matcher mMatcher = M_PATTERN.matcher(value);
		if (mMatcher.matches()) {
			String mString = mMatcher.group(1);
			return parseOsmDecimal(mString, false);
		}

		/* try km measure */

		Matcher kmMatcher = KM_PATTERN.matcher(value);
		if (kmMatcher.matches()) {
			String kmString = kmMatcher.group(1);
			double km = parseOsmDecimal(kmString, false);
			return 1000 * km;
		}

		/* try mi measure */

		Matcher miMatcher = MI_PATTERN.matcher(value);
		if (miMatcher.matches()) {
			String miString = miMatcher.group(1);
			double mi = parseOsmDecimal(miString, false);
			return M_PER_MI * mi;
		}

		/* try feet/inches measure */

		Matcher feetInchesMatcher = FEET_INCHES_PATTERN.matcher(value);
		if (feetInchesMatcher.matches()) {
			String feetString = feetInchesMatcher.group(1);
			String inchesString = feetInchesMatcher.group(2);
			try {
				int feet = Integer.parseInt(feetString);
				int inches = Integer.parseInt(inchesString);
				if (feet >= 0 && inches >= 0 && inches < 12) {
					return M_PER_INCH * (12 * feet + inches);
				}
			} catch (NumberFormatException nfe) {}
		}

		/* all possibilities failed */

		return null;
	}

	/** variant of {@link #parseMeasure(String)} with a default value */
	public static final double parseMeasure(@Nullable String value, double defaultValue) {
		Double result = parseMeasure(value);
		return result == null ? defaultValue : result;
	}

	private static final Pattern T_PATTERN = Pattern.compile("^([\\d\\.]+)\\s*t$");

	/**
	 * parses a weight value given e.g. for the "maxweight" or "maxaxleload" key.
	 *
	 * @return  weight in t; null if value is null or has syntax errors.
	 */
	public static @Nullable Double parseWeight(@Nullable String value) {

		if (value == null) return null;

		/* try numeric weight (implied t) */

		Double weight = parseOsmDecimal(value, false);
		if (weight != null) {
			return weight;
		}

		/* try t weight */

		Matcher tMatcher = T_PATTERN.matcher(value);
		if (tMatcher.matches()) {
			String tString = tMatcher.group(1);
			return parseOsmDecimal(tString, false);
		}

		/* all possibilities failed */

		return null;

	}

	/** variant of {@link #parseWeight(String)} with a default value */
	public static final double parseWeight(@Nullable String value, double defaultValue) {
		Double result = parseWeight(value);
		return result == null ? defaultValue : result;
	}

	private static final Pattern INCLINE_PATTERN = Pattern.compile("^(\\-?\\d+(?:\\.\\d+)?)\\s*%$");

	/**
	 * parses an incline value as given for the "incline" key.
	 *
	 * @return  incline in percents; null if value is null or has syntax errors.
	 */
	public static final @Nullable Double parseIncline(@Nullable String value) {

		if (value == null) return null;

		Matcher inclineMatcher = INCLINE_PATTERN.matcher(value);
		if (inclineMatcher.matches()) {
			String inclineString = inclineMatcher.group(1);
			return parseOsmDecimal(inclineString, true);
		}

		return null;
	}

	/** variant of {@link #parseIncline(String)} with a default value */
	public static final double parseIncline(@Nullable String value, double defaultValue) {
		Double result = parseIncline(value);
		return result == null ? defaultValue : result;
	}

	/**
	 * parses an angular value as given for the "direction" key.
	 *
	 * @return  angle in degrees measured from north, range [0, 360[;
	 *          null if value is null or has syntax errors.
	 */
	public static final @Nullable Double parseAngle(@Nullable String value) {

		if (value == null) return null;

		/* try numeric angle */

		Double measure = parseOsmDecimal(value, false);
		if (measure != null) {
			return measure % 360;
		}

		/* try cardinal directions (represented by letters) */

		switch (value) {
		case "N"  : return   0.0;
		case "NNE": return  22.5;
		case "NE" : return  45.0;
		case "ENE": return  67.5;
		case "E"  : return  90.0;
		case "ESE": return 112.5;
		case "SE" : return 135.0;
		case "SSE": return 157.5;
		case "S"  : return 180.0;
		case "SSW": return 202.5;
		case "SW" : return 225.0;
		case "WSW": return 247.5;
		case "W"  : return 270.0;
		case "WNW": return 292.5;
		case "NW" : return 315.0;
		case "NNW": return 337.5;
		}

		return null;
	}

	/** variant of {@link #parseAngle(String)} with a default value */
	public static final double parseAngle(@Nullable String value, double defaultValue) {
		Double result = parseAngle(value);
		return result == null ? defaultValue : result;
	}

	/**
	 * parses an hexadecimal color value or color name.
	 * Names following the OSM underscore convention (e.g. light_blue) are normalized by removing the underscores.
	 *
	 * @return  color; null if value is null or has syntax errors.
	 */
	public static final @Nullable Color parseColor(@Nullable String value, ColorNameDefinition colorNameDefinition) {
		if (value == null) {
			return null;
		} else {
			String normalizedValue = value.replaceAll("_", "").toLowerCase();
			if (colorNameDefinition.contains(normalizedValue)) {
				return colorNameDefinition.get(normalizedValue);
			} else {
				return parseColor(value);
			}
		}
	}

	/**
	 * parses an hexadecimal color value
	 *
	 * @return  color; null if value is null or has syntax errors.
	 */
	public static final @Nullable Color parseColor(@Nullable String value) {
		if (value == null) return null;
		try {
			return Color.decode(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static final Pattern LEVEL_RANGE_PATTERN = Pattern.compile("([-]?\\d+)-([-]?\\d+)");

	/**
	 * parses a Simple Indoor Tagging level value (for keys like level=* and repeat_on).
	 * Works for integer level values (including negative levels).
	 * Supports ranges and semicolon-separated values in addition to single values.
	 *
	 * @return duplicate-free list of levels, at least one value, ascending. null if value is null or has syntax errors.
	 */
	public static final @Nullable List<Integer> parseLevels(@Nullable String value) {

		if (value == null) return null;

		List<Integer> result = new ArrayList<>(1);

		for (String levelRange : value.replaceAll("\\s+", "").split(";")) {
			try {

				Matcher rangePatternMatcher = LEVEL_RANGE_PATTERN.matcher(levelRange);

				if (rangePatternMatcher.matches()) {
					// range (e.g. "-5-10")
					int lowerLevel = Integer.parseInt(rangePatternMatcher.group(1));
					int upperLevel = Integer.parseInt(rangePatternMatcher.group(2));
					for (int i = lowerLevel; i <= upperLevel; i++) {
						result.add(i);
					}
				} else {
					// single value (e.g. "3")
					result.add(Integer.parseInt(levelRange));
				}

			} catch (NumberFormatException e) {}
		}

		result.sort(null);

		for (int i = result.size() - 1; i >= 1; i--) {
			// remove duplicates (relies on sorted list)
			if (result.get(i) == result.get(i - 1)) {
				result.remove(i);
			}
		}

		return result.isEmpty() ? null : result;

	}

	/** variant of {@link #parseLevels(String)} with a default value */
	public static final List<Integer> parseLevels(@Nullable String value, List<Integer> defaultValue) {
		List<Integer> result = parseLevels(value);
		return result == null ? defaultValue : result;
	}

}
