package org.osm2world.core.map_data.creation;

import org.junit.Test;
import org.osm2world.core.math.VectorXZ;

public class MetricMapProjectionTest {
	
	@Test(expected=IllegalStateException.class)
	public void testCalcPos_missingOrigin() {
		
		OriginMapProjection projection = new MetricMapProjection();
		
		projection.calcPos(new LatLon(1, 2));
		
	}
	
	@Test(expected=IllegalStateException.class)
	public void testCalcLat_missingOrigin() {
		
		OriginMapProjection projection = new MetricMapProjection();
		
		projection.calcLat(new VectorXZ(1, 2));
		
	}
	
	@Test(expected=IllegalStateException.class)
	public void testCalcLon_missingOrigin() {
		
		OriginMapProjection projection = new MetricMapProjection();
		
		projection.calcLon(new VectorXZ(1, 2));
		
	}
	
}
