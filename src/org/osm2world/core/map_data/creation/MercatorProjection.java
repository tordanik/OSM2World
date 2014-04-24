package org.osm2world.core.map_data.creation;

import static java.lang.Math.PI;
import static java.lang.Math.atan;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.tan;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

final class MercatorProjection {

	private MercatorProjection() {
	}

	private static final double R_MAJOR = 6378137.0;
	private static final double R_MINOR = 6356752.3142;
	private static final double RATIO = R_MINOR / R_MAJOR;
	private static final double ECCENT = sqrt(1.0 - (RATIO * RATIO));
	private static final double COM = 0.5 * ECCENT;
	public static final double EARTH_CIRCUMFERENCE = 40075016.686;

	/**
	 *  Calculate earth circumference at given latitude.
	 */
	public static double earthCircumference(double latitude) {
		return EARTH_CIRCUMFERENCE * Math.cos(toRadians(latitude));
	}

	/**
	 * Convert longitude to Mercator projection (range [0..1]).
	 */ 
	public static double lonToX(double longitude) {
		return (longitude + 180.0) / 360.0;
	}

	/**
	 * Convert from Mercator projection (range [0..1]) to longitude.
	 */
	public static double xToLon(double x) {
		return 360.0 * (x - 0.5);
	}

	/**
	 * Convert latitude to Mercator projection (range [0..1]).
	 */ 
	public static double latToY(double latitude) {
		double sinLat = Math.sin(toRadians(latitude));
		return Math.log((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * Math.PI) + 0.5;
	}

	/**
	 * Convert from Mercator projection (range [0..1]) to latitude.
	 */
	public static double yToLat(double y) {
		return 360.0 * Math.atan(Math.exp((y - 0.5) * (2.0 * Math.PI))) / Math.PI - 90.0;
	}
	// This is for the Elliptical Mercator version
	public static double latToYElliptical(double lat) {
		lat = Math.min(89.5, Math.max(lat, -89.5));
		double phi = toRadians(lat);
		double sinphi = sin(phi);
		double con = ECCENT * sinphi;
		con = pow(((1.0 - con) / (1.0 + con)), COM);
		double ts = tan(0.5 * ((PI * 0.5) - phi)) / con;
		return 0 - R_MAJOR * log(ts);
	}

	// This is for the Elliptical Mercator version
	public static double yToLatElliptical(double y) {
		double ts = exp(-y / R_MAJOR);
		double phi = PI / 2 - 2 * atan(ts);
		double dphi = 1.0;
		int i = 0;
		while ((Math.abs(dphi) > 0.000000001) && (i < 15))
		{
			double con = ECCENT * sin(phi);
			dphi = PI / 2 - 2 * atan(ts * Math.pow((1.0 - con) / (1.0 + con), COM)) - phi;
			phi += dphi;
			i++;
		}
		return toDegrees(phi);
	}
}