package org.osm2world.core.map_data.creation;

import static java.lang.Math.*;

final class MercatorProjection {
	
	private MercatorProjection() { }
	
	private static final double R_MAJOR = 6378137.0;
	private static final double R_MINOR = 6356752.3142;
	private static final double RATIO = R_MINOR / R_MAJOR;
	private static final double ECCENT = sqrt(1.0 - (RATIO * RATIO));
	private static final double COM = 0.5 * ECCENT;
	
	public static double lonToX(double lon) {
		return R_MAJOR * toRadians(lon);
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
	
	public static double latToY(double lat) {
		return R_MAJOR * Math.log(Math.tan(PI/4+toRadians(lat)/2));
	}
	
	public static double xToLon(double x) {
		return toDegrees(x) / R_MAJOR;
	}
	
	// This is for the Elliptical Mercator version
	public static double yToLatElliptical(double y) {
		double ts = exp(-y / R_MAJOR);
		double phi = PI/2 - 2 * atan(ts);
		double dphi = 1.0;
		int i = 0;
		while ((Math.abs(dphi) > 0.000000001) && (i < 15))
		{
			double con = ECCENT * sin(phi);
			dphi = PI/2 - 2 * atan(ts * Math.pow((1.0 - con) / (1.0 + con), COM)) - phi;
			phi += dphi;
			i++;
		}
		return toDegrees(phi);
	}
	
	public static double yToLat(double y) {
		return toDegrees(2.0 * atan(exp(y / R_MAJOR)) - PI/2);
	}

}