package org.cesiumjs;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.geo.LatLon;

/**
 * This code is adaptation of Cesium.js code.
 * See LICENSE.md
 * */
public class WGS84Util {

	private static final double WGS84_A = 6378137.0;
	private static final double WGS84_B = 6356752.3142451793;

	private static final VectorXYZ oneOverRadiiSquared = new VectorXYZ(
			1.0 / (WGS84_A * WGS84_A), 
			1.0 / (WGS84_A * WGS84_A), 
			1.0 / (WGS84_B * WGS84_B));
	
	private static final VectorXYZ wgs84RadiiSquared = 
			new VectorXYZ(WGS84_A * WGS84_A, WGS84_A * WGS84_A, WGS84_B * WGS84_B);
	
	public static VectorXYZ cartesianFromLatLon(LatLon origin, double height) {
		double latitude = Math.toRadians(origin.lat);
		double longitude = Math.toRadians(origin.lon);
		VectorXYZ radiiSquared = wgs84RadiiSquared;

        double cosLatitude = Math.cos(latitude);
        VectorXYZ scratchN = new VectorXYZ(
        		cosLatitude * Math.cos(longitude), 
        		cosLatitude * Math.sin(longitude),
        		Math.sin(latitude));
        
        scratchN = scratchN.normalize();

        VectorXYZ scratchK = mulByComponents(radiiSquared, scratchN);
        double gamma = Math.sqrt(scratchN.dot(scratchK));
        scratchK = scratchK.mult(1.0 / gamma);
        scratchN = scratchN.mult(height);

        return scratchK.add(scratchN);
	}
	
	public static double[] eastNorthUpToFixedFrame(VectorXYZ cartesian) {
		
		VectorXYZ normal = geodeticSurfaceNormal(cartesian);
		VectorXYZ tangent = new VectorXYZ(-cartesian.y, cartesian.x, 0.0).normalize();
		VectorXYZ bitangent = normal.cross(tangent);
		
		// Matrix 4x4 by columns 
		return new double[] {
				tangent.x,
		        tangent.y,
		        tangent.z,
		        0.0,
		        bitangent.x,
		        bitangent.y,
		        bitangent.z,
		        0.0,
		        normal.x,
		        normal.y,
		        normal.z,
		        0.0,
		        cartesian.x,
		        cartesian.y,
		        cartesian.z,
		        1.0
		};
	}
	
	public static VectorXYZ geodeticSurfaceNormal(VectorXYZ cartesian) {
		VectorXYZ mulByComponents = mulByComponents(cartesian, oneOverRadiiSquared);
		return mulByComponents.normalize();
	}
	
	public static VectorXYZ mulByComponents(VectorXYZ a, VectorXYZ b) {
		return new VectorXYZ(a.x * b.x, a.y * b.y, a.z * b.z);
	}
	
}
