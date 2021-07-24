package org.osm2world.core.map_data.creation;

public class MetricMapProjectionTest extends AbstractMapProjectionTest {

	@Override
	protected MapProjection createProjection(LatLon origin) {
		return new MetricMapProjection(origin);
	}

}
