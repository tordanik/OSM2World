package org.osm2world.core.map_data.creation;

import static java.lang.Math.*;

import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.osm.data.OSMNode;

/**
 * application of an orthographic projection that is intended to use
 * values in meters centered around the coordinate center (0,0).
 * It projects coordinates onto a plane touching the globe at one of the
 * {@link OSMNode}s in the data. This results in sufficient accuracy
 * if the data covers only a "small" part of the globe.
 */
public class OrthographicAzimuthalMapProjection implements MapProjection {
	
	private final double GLOBE_RADIUS = 6371000;
	
	/** the point where the plane touches the globe */
	private final double lat0, lon0;
	
	public OrthographicAzimuthalMapProjection(double lat0Deg, double lon0Deg) {
		this.lat0 = toRadians(lat0Deg);
		this.lon0 = toRadians(lon0Deg);
	}
	
	public OrthographicAzimuthalMapProjection(OSMData osmData) {
		
		if (osmData.getNodes().isEmpty()) {
			throw new IllegalArgumentException("OSM data must contain nodes");
		}
		
		OSMNode firstNode = osmData.getNodes().iterator().next();
		lon0 = toRadians(firstNode.lon);
		lat0 = toRadians(firstNode.lat);
		
	}
	
	public VectorXZ calcPos(double latDeg, double lonDeg) {
		
		double lat = toRadians(latDeg);
		double lon = toRadians(lonDeg);
		
		double x = GLOBE_RADIUS * cos(lat) * sin(lon - lon0);
		double y = GLOBE_RADIUS * (cos(lat0) * sin(lat) - sin(lat0) * cos(lat) * cos(lon - lon0));
		
		return new VectorXZ(x, y);
		
	}
	
	@Override
	public double calcLat(VectorXZ pos) {
		
		double rho = sqrt(pos.x * pos.x + pos.z * pos.z);
		double c = asin(rho / GLOBE_RADIUS);
		
		if (rho > 0) {
			return asin( cos(c) * sin(lat0) + ( pos.z * sin(c) * cos(lat0) ) / rho );
		} else {
			return lat0;
		}
		
	}
	
	@Override
	public double calcLon(VectorXZ pos) {
		
		double rho = sqrt(pos.x * pos.x + pos.z * pos.z);
		double c = asin(rho / GLOBE_RADIUS);
		
		double div = rho * cos(lat0) * cos(c) - pos.z * sin(lat0) * sin(c);
		
		if (abs(div) > 1e-5) {
			return lon0 + atan2( pos.x * sin(c), div );
		} else {
			return lon0;
		}
		
	}
	
	@Override
	public VectorXZ getNorthUnit() {
		return VectorXZ.Z_UNIT;
	}
	
}
