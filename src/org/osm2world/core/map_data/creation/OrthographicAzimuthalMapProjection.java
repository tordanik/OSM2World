package org.osm2world.core.map_data.creation;

import static java.lang.Math.*;

import org.osm2world.core.math.VectorXZ;

/**
 * application of an orthographic projection that is intended to use
 * values in meters centered around the coordinate center (0,0).
 * It projects coordinates onto a plane touching the globe at the origin.
 * This results in sufficient accuracy
 * if the data covers only a "small" part of the globe.
 */
public class OrthographicAzimuthalMapProjection extends OriginMapProjection {
	
	private final double GLOBE_RADIUS = 6371000;
			
	@Override
	public VectorXZ calcPos(LatLon latlon) {
		return calcPos(latlon.lat, latlon.lon);
	}
	
	@Override
	public VectorXZ calcPos(double latDeg, double lonDeg) {
		
		double lat = toRadians(latDeg);
		double lon = toRadians(lonDeg);
		
		double x = GLOBE_RADIUS * cos(lat) * sin(lon - origin.lon);
		double y = GLOBE_RADIUS * (cos(origin.lat) * sin(lat) - sin(origin.lat) * cos(lat) * cos(lon - origin.lon));
		
		return new VectorXZ(x, y);
		
	}
	
	@Override
	public double calcLat(VectorXZ pos) {
		
		double rho = sqrt(pos.x * pos.x + pos.z * pos.z);
		double c = asin(rho / GLOBE_RADIUS);
		
		if (rho > 0) {
			return asin( cos(c) * sin(origin.lat) + ( pos.z * sin(c) * cos(origin.lat) ) / rho );
		} else {
			return origin.lat;
		}
		
	}
	
	@Override
	public double calcLon(VectorXZ pos) {
		
		double rho = sqrt(pos.x * pos.x + pos.z * pos.z);
		double c = asin(rho / GLOBE_RADIUS);
		
		double div = rho * cos(origin.lat) * cos(c) - pos.z * sin(origin.lat) * sin(c);
		
		if (abs(div) > 1e-5) {
			return origin.lon + atan2( pos.x * sin(c), div );
		} else {
			return origin.lon;
		}
		
	}
	
	@Override
	public VectorXZ getNorthUnit() {
		return VectorXZ.Z_UNIT;
	}
	
}
