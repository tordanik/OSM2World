package org.osm2world.math.geo;

public class MetricMapProjectionTest extends AbstractMapProjectionTest {

	@Override
	protected MapProjection createProjection(LatLon origin) {
		return new MetricMapProjection(origin);
	}

}
