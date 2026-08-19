package org.osm2world.math.geo;

import static java.lang.Math.*;

import org.osm2world.math.VectorXYZ;

/**
 * Utility class for the WGS84 reference ellipsoid and Earth-centered, Earth-fixed (ECEF) coordinates.
 */
public final class WGS84Util {

	/** semi-major axis (equatorial radius) of the WGS84 ellipsoid in meters */
	static final double SEMI_MAJOR_AXIS = 6_378_137.0;

	/** inverse of the WGS84 ellipsoid's flattening */
	private static final double INVERSE_FLATTENING = 298.257223563;

	/** square of the ellipsoid's first eccentricity, i.e. 1 - (b/a)² */
	static final double ECCENTRICITY_SQ = (2 - 1 / INVERSE_FLATTENING) / INVERSE_FLATTENING;

	/** semi-minor axis (polar radius) of the WGS84 ellipsoid in meters */
	private static final double SEMI_MINOR_AXIS = SEMI_MAJOR_AXIS * (1 - 1 / INVERSE_FLATTENING);

	/** square of the ellipsoid's second eccentricity, i.e. (a/b)² - 1 */
	private static final double SECOND_ECCENTRICITY_SQ = ECCENTRICITY_SQ / (1 - ECCENTRICITY_SQ);

	private WGS84Util() {}

	/** returns the ellipsoid's radius of curvature in the prime vertical, given the sine of a latitude */
	static double primeVerticalRadius(double sinLat) {
		return SEMI_MAJOR_AXIS / sqrt(1 - ECCENTRICITY_SQ * sinLat * sinLat);
	}

	/**
	 * converts geodetic coordinates to ECEF coordinates.
	 *
	 * @param pos  latitude and longitude
	 * @param ele  height above the ellipsoid in meters
	 *
	 * @return ECEF coordinates. Represented as {@link VectorXYZ}, but the axes have a different meaning than
	 * elsewhere in OSM2World: x points at (0°N, 0°E), y at (0°N, 90°E), and z at the North Pole.
	 * All distances are in meters.
	 */
	static VectorXYZ ecefFromLatLon(LatLon pos, double ele) {

		double sinLat = sin(toRadians(pos.lat)), cosLat = cos(toRadians(pos.lat));
		double sinLon = sin(toRadians(pos.lon)), cosLon = cos(toRadians(pos.lon));

		double n = primeVerticalRadius(sinLat);

		return new VectorXYZ(
				(n + ele) * cosLat * cosLon,
				(n + ele) * cosLat * sinLon,
				(n * (1 - ECCENTRICITY_SQ) + ele) * sinLat);

	}

	/**
	 * converts ECEF coordinates to geodetic coordinates, the inverse of {@link #ecefFromLatLon(LatLon, double)}.
	 * Uses Bowring's method, which is accurate to well below a millimeter for points near the ellipsoid's surface.
	 * The height above the ellipsoid is not part of the result.
	 *
	 * @param pos  ECEF coordinates, with the axes as documented for {@link #ecefFromLatLon(LatLon, double)}
	 */
	static LatLon latLonFromEcef(VectorXYZ pos) {

		/* distance from the polar axis */
		double p = sqrt(pos.x * pos.x + pos.y * pos.y);

		/* sine and cosine of the parametric latitude of the point's projection onto the ellipsoid.
		 * This is atan2(pos.z * a, p * b) resolved without ever forming the angle itself. */
		double u = pos.z * SEMI_MAJOR_AXIS, v = p * SEMI_MINOR_AXIS;
		double hypot = sqrt(u * u + v * v);
		double sinTheta = u / hypot, cosTheta = v / hypot;

		double lat = atan2(
				pos.z + SECOND_ECCENTRICITY_SQ * SEMI_MINOR_AXIS * sinTheta * sinTheta * sinTheta,
				p - ECCENTRICITY_SQ * SEMI_MAJOR_AXIS * cosTheta * cosTheta * cosTheta);

		return new LatLon(toDegrees(lat), toDegrees(atan2(pos.y, pos.x)));

	}

	/**
	 * intersects a ray with the surface of the ellipsoid.
	 *
	 * @param start  starting point of the ray in ECEF coordinates, on or above the surface
	 * @param direction  unit vector for the ray's direction, pointing towards the surface
	 *
	 * @return  the intersection closest to the ray's starting point, or, if the ray misses the ellipsoid,
	 *          the point on the ray which comes closest to the surface
	 */
	static VectorXYZ intersectWithSurface(VectorXYZ start, VectorXYZ direction) {

		double aSq = SEMI_MAJOR_AXIS * SEMI_MAJOR_AXIS;
		double bSq = SEMI_MINOR_AXIS * SEMI_MINOR_AXIS;

		/* insert the ray into the ellipsoid's equation, resulting in a quadratic equation for the ray parameter */

		double a = (direction.x * direction.x + direction.y * direction.y) / aSq + direction.z * direction.z / bSq;
		double b = 2 * ((start.x * direction.x + start.y * direction.y) / aSq + start.z * direction.z / bSq);
		double c = (start.x * start.x + start.y * start.y) / aSq + start.z * start.z / bSq - 1;

		double discriminant = b * b - 4 * a * c;

		double t = (discriminant > 0)
				? (-b - sqrt(discriminant)) / (2 * a)
				: -b / (2 * a);

		return start.add(direction.mult(t));

	}

	/**
	 * builds the transformation from a local east-north-up (ENU) coordinate system to ECEF coordinates.
	 * The ENU system has its origin at the given position, with its axes pointing east, north,
	 * and up along the ellipsoid's surface normal.
	 *
	 * @param origin  latitude and longitude of the ENU system's origin
	 * @param ele  height of the ENU system's origin above the ellipsoid in meters
	 *
	 * @return  4x4 matrix in column-major order (the layout expected by glTF and 3D Tiles):
	 *          the east, north and up axes, followed by the origin's ECEF coordinates
	 */
	public static double[] eastNorthUpToEcefMatrix(LatLon origin, double ele) {

		double sinLat = sin(toRadians(origin.lat)), cosLat = cos(toRadians(origin.lat));
		double sinLon = sin(toRadians(origin.lon)), cosLon = cos(toRadians(origin.lon));

		VectorXYZ pos = ecefFromLatLon(origin, ele);

		return new double[] {
				-sinLon,          cosLon,           0,      0,  // east
				-sinLat * cosLon, -sinLat * sinLon, cosLat, 0,  // north
				cosLat * cosLon,  cosLat * sinLon,  sinLat, 0,  // up
				pos.x,            pos.y,            pos.z,  1   // origin
		};

	}

}
