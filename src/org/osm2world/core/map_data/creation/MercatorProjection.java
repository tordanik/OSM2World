package org.osm2world.core.map_data.creation;

public final class MercatorProjection {
	
	private MercatorProjection() { }
	
	private static final double R_MAJOR = 6378137.0;
	private static final double R_MINOR = 6356752.3142;
	private static final double RATIO = R_MINOR / R_MAJOR;
	private static final double ECCENT = Math.sqrt(1.0 - (RATIO * RATIO));
	private static final double COM = 0.5 * ECCENT;
	
	private static final double DEG2RAD = Math.PI / 180.0;
	private static final double RAD2Deg = 180.0 / Math.PI;
	private static final double PI_2 = Math.PI / 2.0;
	
	public static double lonToX(double lon) {
		return R_MAJOR * DegToRad(lon);
	}
	
	// This is for the Elliptical Mercator version
	public static double latToYElliptical(double lat) {
		lat = Math.min(89.5, Math.max(lat, -89.5));
		double phi = DegToRad(lat);
		double sinphi = Math.sin(phi);
		double con = ECCENT * sinphi;
		con = Math.pow(((1.0 - con) / (1.0 + con)), COM);
		double ts = Math.tan(0.5 * ((Math.PI * 0.5) - phi)) / con;
		return 0 - R_MAJOR * Math.log(ts);
	}
	
	public static double latToY(double lat) {
		return R_MAJOR * Math.log(Math.tan(Math.PI/4+DegToRad(lat)/2));
	}
	
	public static double xToLon(double x) {
		return RadToDeg(x) / R_MAJOR;
	}
	
	// This is for the Elliptical Mercator version
	public static double yToLatElliptical(double y) {
		double ts = Math.exp(-y / R_MAJOR);
		double phi = PI_2 - 2 * Math.atan(ts);
		double dphi = 1.0;
		int i = 0;
		while ((Math.abs(dphi) > 0.000000001) && (i < 15))
		{
			double con = ECCENT * Math.sin(phi);
			dphi = PI_2 - 2 * Math.atan(ts * Math.pow((1.0 - con) / (1.0 + con), COM)) - phi;
			phi += dphi;
			i++;
		}
		return RadToDeg(phi);
	}
	
	public static double yToLat(double y) {
		return RadToDeg(2.0 * Math.atan(Math.exp(y / R_MAJOR)) - Math.PI/2);
	}
	
	private static double RadToDeg(double rad) {
		return rad * RAD2Deg;
	}
	
	private static double DegToRad(double deg) {
		return deg * DEG2RAD;
	}
}