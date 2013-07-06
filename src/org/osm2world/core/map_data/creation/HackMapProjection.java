package org.osm2world.core.map_data.creation;

import static org.osm2world.core.map_data.creation.MercatorProjection.*;

import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.osm.data.OSMNode;

/**
 * quick-and-dirty projection that is intended to use the "dense" space
 * of floating point values, and tries to make 1 meter the distance
 * represented by 1 internal unit
 */
public class HackMapProjection implements MapProjection {
	
	/**
	 * This is a correction value to make lonToX-values metric values
	 */
	private final static double LON_CORRECTION = 1.5;
	
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
	
	public HackMapProjection(LatLon origin) {
		this.originLat = origin.lat;
		this.originLon = origin.lon;
	}
	
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
		
		double x = lonToX(lon - originLon) / LON_CORRECTION;
		double y = latToY(lat - originLat);
		
		return new VectorXZ(x, y); //x and z(!) are 2d here
	}
	
	@Override
	public VectorXZ calcPos(LatLon latlon) {
		return calcPos(latlon.lat, latlon.lon);
	}
	
	@Override
	public double calcLat(VectorXZ pos) {
		return yToLat(pos.z) + originLat;
	}
	
	@Override
	public double calcLon(VectorXZ pos) {
		return xToLon(pos.x * LON_CORRECTION) + originLon;
	}
	
	@Override
	public VectorXZ getNorthUnit() {
		return VectorXZ.Z_UNIT;
	}
	
}
