package org.osm2world.core.map_data.creation;

import static java.lang.Double.NaN;
import static java.lang.Math.*;

import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.osm.data.OSMData;

/**
 * application of an orthographic projection that is intended to use
 * values in meters centered around the coordinate center (0,0).
 * It projects coordinates onto a plane touching the globe at the origin.
 * This results in sufficient accuracy
 * if the data covers only a "small" part of the globe.
 */
public class OrthographicAzimuthalMapProjection extends OriginMapProjection {
	
	private final double GLOBE_RADIUS = 6371000;
	
	private double lat0 = NaN;
	private double lon0 = NaN;
	
	@Override
	public VectorXZ calcPos(LatLon latlon) {
		return calcPos(latlon.lat, latlon.lon);
	}
	
	@Override
	public void setOrigin(LatLon origin) {
		
		super.setOrigin(origin);
		
		lat0 = toRadians(getOrigin().lat);
		lon0 = toRadians(getOrigin().lon);
		
	}
	
	@Override
	public void setOrigin(OSMData osmData) {
		
		super.setOrigin(osmData);
		
		lat0 = toRadians(getOrigin().lat);
		lon0 = toRadians(getOrigin().lon);
		
	}
	
	@Override
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
			return toDegrees(asin( cos(c) * sin(lat0) + ( pos.z * sin(c) * cos(lat0) ) / rho ));
		} else {
			return toDegrees(lat0);
		}
		
	}
	
	@Override
	public double calcLon(VectorXZ pos) {
		
		double rho = sqrt(pos.x * pos.x + pos.z * pos.z);
		double c = asin(rho / GLOBE_RADIUS);
		
		double div = rho * cos(lat0) * cos(c) - pos.z * sin(lat0) * sin(c);
		
		if (abs(div) > 1e-5) {
			return toDegrees(lon0 + atan2( pos.x * sin(c), div ));
		} else {
			return toDegrees(lon0);
		}
		
	}
	
	@Override
	public VectorXZ getNorthUnit() {
		return VectorXZ.Z_UNIT;
	}
	
}
