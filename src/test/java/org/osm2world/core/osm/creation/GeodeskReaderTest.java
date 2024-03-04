package org.osm2world.core.osm.creation;

import org.junit.Test;
import org.osm2world.core.map_data.creation.LatLonBounds;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.target.common.rendering.TileNumber;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class GeodeskReaderTest {

	private static final LatLonBounds globalBounds = new LatLonBounds(-90, -180, 90, 180);

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

	@Test
	public void testTile() throws IOException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		URL testFile = classLoader.getResource("simpleTest01.gol");
		assertNotNull(testFile);
		GeodeskReader reader = new GeodeskReader(new File(testFile.getFile()),
				new TileNumber(13, 4402, 2828).bounds());

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
