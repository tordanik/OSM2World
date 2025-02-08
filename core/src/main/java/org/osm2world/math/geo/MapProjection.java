package org.osm2world.math.geo;

import org.osm2world.math.VectorXZ;

/**
 * projection which converts {@link LatLon} coordinates to OSM2World's internal {@link VectorXZ} coordinate system,
 * and allows the inverse calculation as well.
 * <p>
 * In OSM2World coordinates, 1 unit of distance along the X, Y or Z axis corresponds to approximately 1 meter.
 * The X axis points east, the Z axis points north.
 * <p>
 * OSM2World's map projections are intended to use the "dense" space of floating point values by making all coordinates
 * relative to a suitable origin. Because OSM2World only processes relatively small parts of the globe at a time,
 * projections which only work well locally around the origin are well suited.
 */
public interface MapProjection {

	/** performs projection into the internal coordinate system */
	default public VectorXZ toXZ(LatLon latlon) {
		return toXZ(latlon.lat, latlon.lon);
	}

	/** performs projection into the internal coordinate system */
	public VectorXZ toXZ(double lat, double lon);

	/** inverse for {@link #toXZ(LatLon)} */
	default public LatLon toLatLon(VectorXZ pos) {
		return new LatLon(toLat(pos), toLon(pos));
	}

	/** returns only the latitude of {@link #toLatLon(VectorXZ)} */
	public double toLat(VectorXZ pos);

	/** returns only the longitude of {@link #toLatLon(VectorXZ)} */
	public double toLon(VectorXZ pos);

	/** returns the origin, i.e. the {@link LatLon} that maps to (0,0) */
	public LatLon getOrigin();

}
