package org.osm2world.core.map_data.creation;

public final class MercatorProjection {
	
	public static void main(String[] args) {
		
		//TODO: remove debug code
		
		System.out.println(latToY(0));
		System.out.println(latToY(30));
		System.out.println(latToY(60));
		System.out.println(latToY(90));
				
		System.out.println(lonToX(0));
		System.out.println(lonToX(90));
		System.out.println(lonToX(180));
	}
	
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
 
    public static double latToY(double lat) {
        lat = Math.min(89.5, Math.max(lat, -89.5));
        double phi = DegToRad(lat);
        double sinphi = Math.sin(phi);
        double con = ECCENT * sinphi;
        con = Math.pow(((1.0 - con) / (1.0 + con)), COM);
        double ts = Math.tan(0.5 * ((Math.PI * 0.5) - phi)) / con;
        return 0 - R_MAJOR * Math.log(ts);
    }
 
    public static double xToLon(double x) {
        return RadToDeg(x) / R_MAJOR;
    }
 
    public static double yToLat(double y) {
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
 
    private static double RadToDeg(double rad) {
        return rad * RAD2Deg;
    }
 
    private static double DegToRad(double deg) {
        return deg * DEG2RAD;
    }
    
}