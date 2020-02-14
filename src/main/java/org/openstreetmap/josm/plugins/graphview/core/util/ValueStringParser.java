package org.openstreetmap.josm.plugins.graphview.core.util;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osm2world.core.util.ColorNameDefinition;

public final class ValueStringParser {

	/** prevents instantiation */
	private ValueStringParser() { }

	/** pattern that splits into a part before and after a decimal point */
	private static final Pattern DEC_POINT_PATTERN = Pattern.compile("^(\\-?\\d+)\\.(\\d+)$");

	public static final Integer parseUInt(String value) {
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
	public static final int parseUInt(String value, int defaultValue) {
		if (value == null) return defaultValue;
		Integer result = parseUInt(value);
		return result == null ? defaultValue : result;
	}

	public static final Float parseOsmDecimal(String value, boolean allowNegative) {

		/* positive integer */

		try {

			int weight = Integer.parseInt(value);
			if (weight >= 0 || allowNegative) {
				return (float)weight;
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

					float beforePoint = Integer.parseInt(stringBeforePoint);
					float afterPoint = Integer.parseInt(stringAfterPoint);

					double result = Math.abs(beforePoint)
							+ Math.pow(10, -stringAfterPoint.length()) * afterPoint;
					if (negative) { result = - result; }

					if (result >= 0 || allowNegative) {
						return (float)result;
					}

				} catch (NumberFormatException nfe) {}

			}
		}

		return null;
	}

	/** variant of {@link #parseOsmDecimal(String, boolean)} with a default value */
	public static final double parseOsmDecimal(String value, boolean allowNegative, double defaultValue) {
		if (value == null) return defaultValue;
		Float result = parseOsmDecimal(value, allowNegative);
		return result == null ? defaultValue : result;
	}


	private static final Pattern KMH_PATTERN = Pattern.compile("^(\\d+)\\s*km/h$");
	private static final Pattern MPH_PATTERN = Pattern.compile("^(\\d+)\\s*mph$");

	private static final float KM_PER_MILE = 1.609344f;

	/**
	 * parses a speed value given e.g. for the "maxspeed" key.
	 *
	 * @return  speed in km/h; null if value had syntax errors
	 */
	public static final Float parseSpeed(String value) {

		/* try numeric speed (implied km/h) */

		Float speed = parseOsmDecimal(value, false);
		if (speed != null) {
			return speed;
		}

		/* try km/h speed */

		Matcher kmhMatcher = KMH_PATTERN.matcher(value);
		if (kmhMatcher.matches()) {
			String kmhString = kmhMatcher.group(1);
			try {
				return (float)Integer.parseInt(kmhString);
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
	public static final double parseSpeed(String value, double defaultValue) {
		if (value == null) return defaultValue;
		Float result = parseSpeed(value);
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
	 * @return  measure in m; null if value had syntax errors
	 */
	public static final Float parseMeasure(String value) {

		/* try numeric measure (implied m) */

		Float measure = parseOsmDecimal(value, false);
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
			float km = parseOsmDecimal(kmString, false);
			return 1000 * km;
		}

		/* try mi measure */

		Matcher miMatcher = MI_PATTERN.matcher(value);
		if (miMatcher.matches()) {
			String miString = miMatcher.group(1);
			float mi = parseOsmDecimal(miString, false);
			return (float)(M_PER_MI * mi);
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
					return (float)(M_PER_INCH * (12 * feet + inches));
				}
			} catch (NumberFormatException nfe) {}
		}

		/* all possibilities failed */

		return null;
	}

	/** variant of {@link #parseMeasure(String)} with a default value */
	public static final double parseMeasure(String value, double defaultValue) {
		if (value == null) return defaultValue;
		Float result = parseMeasure(value);
		return result == null ? defaultValue : result;
	}

	private static final Pattern T_PATTERN = Pattern.compile("^([\\d\\.]+)\\s*t$");

	/**
	 * parses a weight value given e.g. for the "maxweight" or "maxaxleload" key.
	 *
	 * @return  weight in t; null if value had syntax errors
	 */
	public static Float parseWeight(String value) {

		/* try numeric weight (implied t) */

		Float weight = parseOsmDecimal(value, false);
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
	public static final double parseWeight(String value, double defaultValue) {
		if (value == null) return defaultValue;
		Float result = parseWeight(value);
		return result == null ? defaultValue : result;
	}

	private static final Pattern INCLINE_PATTERN = Pattern.compile("^(\\-?\\d+(?:\\.\\d+)?)\\s*%$");

	/**
	 * parses an incline value as given for the "incline" key.
	 *
	 * @return  incline in percents; null if value had syntax errors
	 */
	public static final Float parseIncline(String value) {

		Matcher inclineMatcher = INCLINE_PATTERN.matcher(value);
		if (inclineMatcher.matches()) {
			String inclineString = inclineMatcher.group(1);
			return parseOsmDecimal(inclineString, true);
		}

		return null;
	}

	/** variant of {@link #parseIncline(String)} with a default value */
	public static final double parseIncline(String value, double defaultValue) {
		if (value == null) return defaultValue;
		Float result = parseIncline(value);
		return result == null ? defaultValue : result;
	}

	/**
	 * parses an angular value as given for the "direction" key.
	 *
	 * @return  angle in degrees measured from north, range [0, 360[;
	 *          null if value had syntax errors
	 */
	public static final Float parseAngle(String value) {

		/* try numeric angle */

		Float measure = parseOsmDecimal(value, false);
		if (measure != null) {
			return measure % 360;
		}

		/* try cardinal directions (represented by letters) */

		switch (value) {
		case "N"  : return   0.0f;
		case "NNE": return  22.5f;
		case "NE" : return  45.0f;
		case "ENE": return  67.5f;
		case "E"  : return  90.0f;
		case "ESE": return 112.5f;
		case "SE" : return 135.0f;
		case "SSE": return 157.5f;
		case "S"  : return 180.0f;
		case "SSW": return 202.5f;
		case "SW" : return 225.0f;
		case "WSW": return 247.5f;
		case "W"  : return 270.0f;
		case "WNW": return 292.5f;
		case "NW" : return 315.0f;
		case "NNW": return 337.5f;
		}

		return null;
	}

	/** variant of {@link #parseAngle(String)} with a default value */
	public static final double parseAngle(String value, double defaultValue) {
		if (value == null) return defaultValue;
		Float result = parseAngle(value);
		return result == null ? defaultValue : result;
	}

	/**
	 * parses an hexadecimal color value or color name.
	 * Names following the OSM underscore convention (e.g. light_blue) are normalized by removing the underscores.
	 *
	 * @return  color; null if value had syntax errors or was null
	 */
	public static final Color parseColor(String value, ColorNameDefinition colorNameDefinition) {
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
	 * @return  color; null if value had syntax errors
	 */
	public static final Color parseColor(String value) {

		try {
			return Color.decode(value);
		} catch (NumberFormatException e) {
			return null;
		}

	}

}
