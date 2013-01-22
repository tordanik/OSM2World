package org.osm2world.core.map_data.creation;

import java.awt.geom.Point2D;

import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.osm.data.OSMNode;

import com.jhlabs.map.proj.MercatorProjection;
import com.jhlabs.map.proj.Projection;

/**
 * quick-and-dirty projection that is intended to use the "dense" space
 * of floating point values, and tries to make 1 meter the distance
 * represented by 1 internal unit
 */
public class HackMapProjection implements MapProjection {
	
	private final Projection projection = new MercatorProjection();
	
	/**
	 * The coordinate origin is placed at the center of the bounds,
	 * or else at the first node's coordinates.
	 * All coordinates will be modified by subtracting the origin
	 * (in lat/lon, which does not really make sense, but is simply
	 *  supposed to keep nodes as close as possible to 0.0).
	 * 
	 * //TODO: replace this solution later
	 */
	private final Double originLat, originLon;

	/* magic constant */
	public static final double SCALE_X = 70000;
	public static final double SCALE_Y = 110000;
	
	public HackMapProjection(OSMData osmData) {
		
		if (osmData.getBounds() != null && !osmData.getBounds().isEmpty()) {
			
			Bound firstBound = osmData.getBounds().iterator().next();
			originLat = (firstBound.getTop() + firstBound.getBottom()) / 2;
			originLon = (firstBound.getLeft() + firstBound.getRight()) / 2;
			
		} else {
			
			if (osmData.getNodes().isEmpty()) {
				throw new IllegalArgumentException("OSM data must contain nodes");
			}
			OSMNode firstNode = osmData.getNodes().iterator().next();
			originLat = firstNode.lat;
			originLon = firstNode.lon;
			
		}
	}

	public VectorXZ calcPos(double lat, double lon) {
				
		lat -= originLat;
		lon -= originLon;
		
//		return new VectorXZ(
//				(float)conversion.MercatorProjection.lonToX(lon),
//				(float)conversion.MercatorProjection.latToY(lat)
//		);
		
		//TODO: maybe remove this projection code and the associated LIB?
		
		Point2D.Double point = new Point2D.Double();
		if (Double.isNaN(projection.project(lon, lat, point).y * SCALE_Y)) {
			System.out.println("NaN!");
		}
		projection.project(lon, lat, point);
		return new VectorXZ(point.x * SCALE_X, point.y * SCALE_Y); //x and z(!) are 2d here
		
	}
	
	@Override
	public VectorXZ calcPos(LatLon latlon) {
		return calcPos(latlon.lat, latlon.lon);
	}
	
	@Override
	public double calcLat(VectorXZ pos) {
		if (pos.equals(VectorXZ.NULL_VECTOR)) {
			return originLat;
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	@Override
	public double calcLon(VectorXZ pos) {
		if (pos.equals(VectorXZ.NULL_VECTOR)) {
			return originLon;
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	@Override
	public VectorXZ getNorthUnit() {
		return VectorXZ.Z_UNIT;
	}
	
}
