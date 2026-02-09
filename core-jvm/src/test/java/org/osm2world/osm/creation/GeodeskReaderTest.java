package org.osm2world.osm.creation;

import static org.junit.Assert.assertFalse;
import static org.osm2world.util.test.TestFileUtil.getTestFile;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.TileNumber;
import org.osm2world.osm.data.OSMData;

public class GeodeskReaderTest {

	private static final LatLonBounds globalBounds = new LatLonBounds(-90, -180, 90, 180);

	@Test
	public void testSimpleFile() throws IOException {

		File testFile = getTestFile("simpleTest01.gol");
		var reader = new GeodeskReader(testFile);

		OSMData data = reader.getData(globalBounds);

		assertFalse(data.getNodes().isEmpty());
		assertFalse(data.getWays().isEmpty());
		assertFalse(data.getRelations().isEmpty());

	}

	@Test
	public void testTile() throws IOException {

		File testFile = getTestFile("simpleTest01.gol");
		var reader = new GeodeskReader(testFile);

		OSMData data = reader.getData(new TileNumber(13, 4402, 2828).latLonBounds());

		assertFalse(data.getNodes().isEmpty());
		assertFalse(data.getWays().isEmpty());
		assertFalse(data.getRelations().isEmpty());

	}

	@Test(expected = IOException.class)
	public void testMissingFile() throws IOException {
		var reader = new GeodeskReader(new File("noSuchFile.gol"));
		reader.getData(globalBounds);
	}

	@Test
	public void testParallelAccess() {

		ExecutorService executor = Executors.newFixedThreadPool(2);
		AtomicReference<IOException> encounteredException = new AtomicReference<>(null);

		for (int i = 0; i < 2; i++) {
			executor.submit(() -> {
				try {
					testSimpleFile();
				} catch (IOException e) {
					encounteredException.set(e);
				}
			});
		}

		executor.shutdown();

		try {
			executor.awaitTermination(1, TimeUnit.SECONDS);
		} catch (InterruptedException ignored) {}

		if (encounteredException.get() != null) {
			throw new AssertionError("At least one thread encountered an exception ", encounteredException.get());
		}

	}

}
