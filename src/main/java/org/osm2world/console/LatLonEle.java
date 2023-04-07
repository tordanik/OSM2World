package org.osm2world.console;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osm2world.core.map_data.creation.LatLon;

/**
 * immutable latitude/longitude/elevation coordinate triple
 */
public class LatLonEle {

	/**
	 * Rename variable refactoring applied
	 * lat changed to latitude
	 * long changed to longitude
	 * ele changed to elevation
	 */
	public final double latitude;
	public final double longitude;
	public final double elevation;

	/** pattern for parseable arguments */
	public static final String PATTERN = LatLon.PATTERN + ",(" + LatLon.DOUBLE_PATTERN + ")";

	/**
	 * regular constructor
	 */
	public LatLonEle(double latitude, double longitude, double elevation) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.elevation = elevation;
		validateValues();
	}

	/**
	 * regular constructor (with default elevation of 0)
	 */
	public LatLonEle(double latitude, double longitude) {
		this(latitude, longitude, 0);
	}

	/**
	 * parsing constructor
	 * @param arg  command line argument to be parsed; must match {@link #PATTERN} or {@link LatLon#PATTERN}
	 */
	public LatLonEle(String arg) {

		arg = arg.replace('−', '-');

		Matcher mEle = Pattern.compile(PATTERN).matcher(arg);
		Matcher m = Pattern.compile(LatLon.PATTERN).matcher(arg);
		if (mEle.matches()) {
			latitude = Double.parseDouble(mEle.group(1));
			longitude = Double.parseDouble(mEle.group(2));
			elevation = Double.parseDouble(mEle.group(3));
			validateValues();
		} else if (m.matches()) {
			latitude = Double.parseDouble(m.group(1));
			longitude = Double.parseDouble(m.group(2));
			elevation = 0;
			validateValues();
		} else {
			throw new IllegalArgumentException("argument doesn't match: " + arg);
		}
	}

	/**
	 * @throws IllegalArgumentException  for incorrect field values
	 */
	private void validateValues() {
		if (latitude > 90 || latitude < -90 || longitude > 180 || longitude < -180) {
			throw new IllegalArgumentException("not valid: " + latitude + ", " + longitude);
		}
	}

	/** returns just the {@link LatLon} components */
	public LatLon latLon() {
		return new LatLon(latitude, longitude);
	}

	@Override
	public String toString() {
		return latitude + "," + longitude;
	}

}
