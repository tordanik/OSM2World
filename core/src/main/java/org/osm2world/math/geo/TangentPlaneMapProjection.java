package org.osm2world.math.geo;

import static java.lang.Math.*;

import java.util.Objects;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;

/**
 * Projection onto the plane which touches the WGS84 ellipsoid at the origin, with the x axis pointing east
 * and the z axis pointing north. Its results are coordinates in the local east-north-up (ENU) system whose
 * transform to ECEF is built by {@link WGS84Util#eastNorthUpToEcefMatrix(LatLon, double)}, which makes it the
 * projection to use for output formats that position their content with such a transform, such as 3D Tiles.
 * Accuracy degrades with the distance from the origin, so like other {@link MapProjection}s in OSM2World,
 * this is only suitable for data covering a relatively small part of the globe.
 */
public class TangentPlaneMapProjection implements MapProjection {

	private final LatLon origin;

	/** ECEF coordinates of the {@link #origin} */
	private final VectorXYZ originEcef;

	/** ECEF directions of the tangent plane's axes, and of the plane's downward normal */
	private final VectorXYZ east, north, down;

	/** sine of the origin's latitude */
	private final double sinLat0;

	/** the parts of the {@link #toXZ(double, double)} formula which only depend on the origin */
	private final double zSinLatFactor, zConstant;

	public TangentPlaneMapProjection(LatLon origin) {

		this.origin = origin;
		this.originEcef = WGS84Util.ecefFromLatLon(origin, 0);

		double sinLat = sin(toRadians(origin.lat)), cosLat = cos(toRadians(origin.lat));
		double sinLon = sin(toRadians(origin.lon)), cosLon = cos(toRadians(origin.lon));

		this.east = new VectorXYZ(-sinLon, cosLon, 0);
		this.north = new VectorXYZ(-sinLat * cosLon, -sinLat * sinLon, cosLat);
		this.down = new VectorXYZ(-cosLat * cosLon, -cosLat * sinLon, -sinLat);

		this.sinLat0 = sinLat;
		this.zSinLatFactor = cosLat * (1 - WGS84Util.ECCENTRICITY_SQ);
		this.zConstant = WGS84Util.primeVerticalRadius(sinLat) * WGS84Util.ECCENTRICITY_SQ * sinLat * cosLat;

	}

	@Override
	public LatLon getOrigin() {
		return origin;
	}

	@Override
	public VectorXZ toXZ(double lat, double lon) {

		double latRad = toRadians(lat);
		double deltaLonRad = toRadians(lon - origin.lon);

		double sinLat = sin(latRad);
		double sinDeltaLon = sin(deltaLonRad);

		/* latitudes are within ±90°, so their cosine is never negative.
		 * The same is true for the difference in longitude as long as it stays below a quarter of the globe,
		 * which it does for any data this projection is suitable for. */

		double cosLat = sqrt(1 - sinLat * sinLat);
		double cosDeltaLon = (abs(deltaLonRad) < PI / 2)
				? sqrt(1 - sinDeltaLon * sinDeltaLon)
				: cos(deltaLonRad);

		double n = WGS84Util.primeVerticalRadius(sinLat);

		/* this is the ECEF offset from the origin, projected onto the plane's east and north axes.
		 * Writing out those two dot products cancels most of the terms, and everything that only
		 * depends on the origin has been folded into zSinLatFactor and zConstant.
		 * The component along the plane's normal drops out; that is what makes this a projection. */

		double x = n * cosLat * sinDeltaLon;
		double z = n * (zSinLatFactor * sinLat - sinLat0 * cosLat * cosDeltaLon) + zConstant;

		/* snap to mm precision, seems to reduce geometry exceptions */
		x = rint(x * 1000) / 1000.0d;
		z = rint(z * 1000) / 1000.0d;

		return new VectorXZ(x, z);

	}

	@Override
	public LatLon toLatLon(VectorXZ pos) {

		VectorXYZ pointOnPlane = originEcef.add(east.mult(pos.x)).add(north.mult(pos.z));

		/* invert the projection by going back down to the surface along the plane's normal */

		return WGS84Util.latLonFromEcef(WGS84Util.intersectWithSurface(pointOnPlane, down));

	}

	@Override
	public double toLat(VectorXZ pos) {
		return toLatLon(pos).lat;
	}

	@Override
	public double toLon(VectorXZ pos) {
		return toLatLon(pos).lon;
	}

	@Override
	public String toString() {
		return "TangentPlaneMapProjection" + origin;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof TangentPlaneMapProjection other && Objects.equals(origin, other.origin);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(origin);
	}

}
