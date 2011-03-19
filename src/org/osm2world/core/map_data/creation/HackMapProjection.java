package org.osm2world.core.map_data.creation;

import java.awt.geom.Point2D;

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
	
	/** all coordinates will be modified by subtracting the first node's coords;
	 *  this is supposed to keep nodes as close as possible to 0.0.
	 *  (It also means that the first lat/lon values will be at 0.0)
	 *  //TODO: re-evaluate this solution later */
	private final Double firstNodeLat, firstNodeLon;

	/* magic constant */
	public static final double SCALE_X = 70000;
	public static final double SCALE_Y = 110000;
	
	public HackMapProjection(OSMData osmData) {
		if (osmData.getNodes().isEmpty()) {
			throw new IllegalArgumentException("OSM data must contain nodes");
		}
		OSMNode firstNode = osmData.getNodes().iterator().next();
		firstNodeLat = firstNode.lat;
		firstNodeLon = firstNode.lon;
	}

	public VectorXZ calcPos(double lat, double lon) {
				
		lat -= firstNodeLat;
		lon -= firstNodeLon;
		
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
	public double calcLat(VectorXZ pos) {
		if (pos.equals(VectorXZ.NULL_VECTOR)) {
			return firstNodeLat;
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	@Override
	public double calcLon(VectorXZ pos) {
		if (pos.equals(VectorXZ.NULL_VECTOR)) {
			return firstNodeLon;
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	@Override
	public VectorXZ getNorthUnit() {
		return VectorXZ.Z_UNIT;
	}
	
}
