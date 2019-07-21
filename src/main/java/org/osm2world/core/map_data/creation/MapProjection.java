package org.osm2world.core.map_data.creation;

import org.osm2world.core.math.VectorXZ;

/**
 * function that converts latitude/longitude coordinates
 * to internally used x/z coordinates
 */
public interface MapProjection {

	public VectorXZ calcPos(double lat, double lon);

	public VectorXZ calcPos(LatLon latlon);

	/**
	 * inverse for {@link #calcPos(double, double)}
	 */
	public double calcLat(VectorXZ pos);

	/**
	 * inverse for {@link #calcPos(double, double)}
	 */
	public double calcLon(VectorXZ pos);

	/**
	 * returns a vector that points one meter to the north
	 * from the position that becomes the coordinate origin
	 */
	public VectorXZ getNorthUnit();

	/**
	 * returns the origin (i.e. the latlon that maps to (0,0)
	 */
	public LatLon getOrigin();

}
