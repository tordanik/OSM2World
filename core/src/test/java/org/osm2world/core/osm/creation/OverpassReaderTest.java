package org.osm2world.core.osm.creation;

import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.Test;
import org.osm2world.core.map_data.creation.LatLon;
import org.osm2world.core.map_data.creation.LatLonBounds;
import org.osm2world.core.osm.data.OSMData;

public class OverpassReaderTest {

	@Test
	public void testBoundingBox() throws IOException {

		OverpassReader reader = new OverpassReader(new LatLonBounds(
				new LatLon(50.746, 7.154), new LatLon(50.748, 7.157)));

		OSMData data = reader.getData();

		assertFalse(data.getNodes().isEmpty());
		assertFalse(data.getWays().isEmpty());
		assertFalse(data.getRelations().isEmpty());

	}

}
