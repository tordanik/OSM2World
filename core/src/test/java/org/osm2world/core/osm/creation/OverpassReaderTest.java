package org.osm2world.core.osm.creation;

import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.Test;
import org.osm2world.core.math.geo.LatLon;
import org.osm2world.core.math.geo.LatLonBounds;
import org.osm2world.core.osm.data.OSMData;

public class OverpassReaderTest {

	@Test
	public void testBoundingBox() throws IOException {

		var reader = new OverpassReader();

		OSMData data = reader.getData(new LatLonBounds(
				new LatLon(50.746, 7.154), new LatLon(50.748, 7.157)));

		assertFalse(data.getNodes().isEmpty());
		assertFalse(data.getWays().isEmpty());
		assertFalse(data.getRelations().isEmpty());

	}

}
