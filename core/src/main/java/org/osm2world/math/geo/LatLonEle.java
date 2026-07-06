package org.osm2world.math.geo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

/**
 * immutable latitude/longitude/elevation coordinate triple
 *
 * @param lat latitude in degrees
 * @param lon longitude in degrees
 * @param ele elevation in meters
 */
public record LatLonEle(double lat, double lon, double ele) {

	/** pattern for parseable arguments */
	public static final String PATTERN = LatLon.PATTERN + ",(" + LatLon.DOUBLE_PATTERN + ")";

	public LatLonEle {
		if (lat > 90 || lat < -90 || lon > 180 || lon < -180) {
			throw new IllegalArgumentException("not valid: " + lat + ", " + lon);
		}
	}

	/**
	 * parsing constructor
	 * @param arg  command line argument to be parsed; must match {@link #PATTERN} or {@link LatLon#PATTERN}
	 */
	public LatLonEle(String arg) {
		this(parse(arg)[0], parse(arg)[1], parse(arg)[2]);
	}

	public static LatLonEle LonLatEle(double lon, double lat, double ele) {
		return new LatLonEle(lat, lon, ele);
	}

	private static double[] parse(String arg) {

		double[] result;

		arg = arg.replace('−', '-');

		Matcher mEle = Pattern.compile(PATTERN).matcher(arg);
		Matcher m = Pattern.compile(LatLon.PATTERN).matcher(arg);
		if (mEle.matches()) {
			result = new double[] {
					Double.parseDouble(mEle.group(1)),
					Double.parseDouble(mEle.group(2)),
					Double.parseDouble(mEle.group(3))};
		} else if (m.matches()) {
			result = new double[] {
					Double.parseDouble(m.group(1)),
					Double.parseDouble(m.group(2)),
					0};
		} else {
			throw new IllegalArgumentException("argument doesn't match: " + arg);
		}

		return result;

	}

	/** returns just the {@link LatLon} components */
	public LatLon latLon() {
		return new LatLon(lat, lon);
	}

	@Override
	public @Nonnull String toString() {
		return lat + "," + lon + "," + ele;
	}

}
