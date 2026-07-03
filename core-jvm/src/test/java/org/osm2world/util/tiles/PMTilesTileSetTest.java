package org.osm2world.util.tiles;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.osm2world.math.geo.TileNumber;
import org.osm2world.util.platform.uri.HttpUriImplementationJvm;
import org.osm2world.util.test.TestFileUtil;

public class PMTilesTileSetTest {

	static {
		HttpUriImplementationJvm.register();
	}

	@Test
	public void testGetTileData() throws IOException {

		File pmtilesFile = TestFileUtil.getTestFile("terrarium-xyz/terrarium_12_2145_1434.pmtiles");

		var tileSet = new PMTilesTileSet(pmtilesFile.toURI());

		assertTrue(tileSet.tileExists(new TileNumber("13/4290/2868")));
		assertTrue(tileSet.tileExists(new TileNumber("13/4290/2869")));
		assertFalse(tileSet.tileExists(new TileNumber("13/4290/2870")));

	}

	@Test
	public void testGetTileDataFromMultifileSource() throws IOException {

		File pmtilesFile = TestFileUtil.getTestFile("terrarium-xyz/terrarium_12_2145_1434.pmtiles");

		String fileUri = "file:" + pmtilesFile.getAbsolutePath();
		fileUri = fileUri.replace("12_2145_1434", "{z}_{x}_{y}");

		var tileSet = new PMTilesTileSet(new TileUriPattern(fileUri));

		assertTrue(tileSet.tileExists(new TileNumber("13/4290/2868")));
		assertTrue(tileSet.tileExists(new TileNumber("13/4290/2869")));
		assertFalse(tileSet.tileExists(new TileNumber("13/4290/2870")));

	}

}
