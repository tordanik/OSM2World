package org.osm2world.core.math;

import org.osm2world.core.map_data.creation.LatLon;

public class WGS84Util {
	private static final VectorXYZ oneOverRadiiSquared = new VectorXYZ(
			2.458172257647332e-14, 2.458172257647332e-14, 2.4747391015697002e-14);
	
	private static final VectorXYZ wgs84RadiiSquared = 
			new VectorXYZ(6378137.0 * 6378137.0, 6378137.0 * 6378137.0, 6356752.3142451793 * 6356752.3142451793);
	
	public static VectorXYZ cartesianFromLatLon(LatLon origin, double height) {
		double latitude = Math.toRadians(origin.lat);
		double longitude = Math.toRadians(origin.lon);
		VectorXYZ radiiSquared = wgs84RadiiSquared;

        double cosLatitude = Math.cos(latitude);
        VectorXYZ scratchN = new VectorXYZ(
        		cosLatitude * Math.cos(longitude), 
        		cosLatitude * Math.sin(longitude),
        		Math.sin(latitude));
        
        scratchN = normalize(scratchN);

        VectorXYZ scratchK = mulByComponents(radiiSquared, scratchN);
        double gamma = Math.sqrt(dot(scratchN, scratchK));
        scratchK = divideByScalar(scratchK, gamma);
        scratchN = multiplyByScalar(scratchN, height);

        return add(scratchK, scratchN);
	}
	
	public static double[] eastNorthUpToFixedFrame(VectorXYZ cartesian) {
		
		VectorXYZ normal = geodeticSurfaceNormal(cartesian);
		VectorXYZ tangent = normalize(new VectorXYZ(-cartesian.y, cartesian.x, 0.0));
		VectorXYZ bitangent = cross(normal, tangent);
		
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
	
	public static VectorXYZ add(VectorXYZ left, VectorXYZ right) {
		return new VectorXYZ(left.x + right.x, left.y + right.y, left.z + right.z);
	}
	
	public static VectorXYZ divideByScalar(VectorXYZ vector, double scalar) {
		return new VectorXYZ(vector.x / scalar, vector.y / scalar, vector.z / scalar);
	}
	
	public static VectorXYZ multiplyByScalar(VectorXYZ vector, double scalar) {
		return new VectorXYZ(vector.x * scalar, vector.y * scalar, vector.z * scalar);
	}
	
	public static double dot(VectorXYZ left, VectorXYZ right) {
		return left.x * right.x + left.y * right.y + left.z * right.z;
	}
	
	public static VectorXYZ geodeticSurfaceNormal(VectorXYZ cartesian) {
		VectorXYZ mulByComponents = mulByComponents(cartesian, oneOverRadiiSquared);
		return normalize(mulByComponents);
	}
	
	public static VectorXYZ cross(VectorXYZ left, VectorXYZ right) {
		double x = left.y * right.z - left.z * right.y;
		double y = left.z * right.x - left.x * right.z;
		double z = left.x * right.y - left.y * right.x;
		
		return new VectorXYZ(x, y, z);
	}
	
	public static VectorXYZ mulByComponents(VectorXYZ a, VectorXYZ b) {
		return new VectorXYZ(a.x * b.x, a.y * b.y, a.z * b.z);
	}
	
	public static VectorXYZ normalize(VectorXYZ a) {
		double m = magnitude(a);
		return new VectorXYZ(a.x / m, a.y / m, a.z / m);
	}
	
	public static double magnitude(VectorXYZ a) {
		return Math.sqrt(a.x * a.x + a.y * a.y + a.z * a.z);
	}
}
