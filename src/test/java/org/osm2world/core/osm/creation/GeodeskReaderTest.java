package org.osm2world.core.osm.creation;

import org.junit.Ignore;
import org.junit.Test;
import org.osm2world.core.map_data.creation.LatLonBounds;
import org.osm2world.core.osm.data.OSMData;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class GeodeskReaderTest {

	private static final LatLonBounds globalBounds = new LatLonBounds(-90, -180, 90, 180);

	@Ignore
	@Test
	public void testSimpleFile() throws IOException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		URL testFile = classLoader.getResource("simpleTest01.gol");
		assertNotNull(testFile);
		GeodeskReader reader = new GeodeskReader(new File(testFile.getFile()), globalBounds);

		OSMData data = reader.getData();

		assertFalse(data.getNodes().isEmpty());
		assertFalse(data.getWays().isEmpty());
		assertFalse(data.getRelations().isEmpty());

	}

	@Test(expected = IOException.class)
	public void testMissingFile() throws IOException {
		GeodeskReader reader = new GeodeskReader(new File("noSuchFile.gol"), globalBounds);
		reader.getData();
	}

}