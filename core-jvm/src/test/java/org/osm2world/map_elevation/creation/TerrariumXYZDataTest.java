package org.osm2world.map_elevation.creation;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.TileNumber;
import org.osm2world.scene.color.Color;
import org.osm2world.util.platform.image.ImageImplementationJvm;
import org.osm2world.util.test.TestFileUtil;
import org.osm2world.util.tiles.TileUriPattern;

public class TerrariumXYZDataTest {

	static {
		ImageImplementationJvm.register();
	}

	@Test
	public void testDecodeValue() {
		assertEquals(2523.265625, TerrariumXYZData.decodeValue(new Color(137, 219, 68).getRGB()), 0.0);
	}

	@Test
	public void testGetSites() throws IOException {

		File tileDir = TestFileUtil.getTestFile("terrarium-xyz");
		assertTrue(tileDir.isDirectory());

		var eleData = new TerrariumXYZData(new TileUriPattern("file://" + tileDir.getAbsolutePath()
				+ File.separator + "{z}" + File.separator + "{x}" + File.separator + "{y}.webp"));

		var bounds = new LatLonBounds(47.385, 8.566, 47.386, 8.567);
		assertFalse(eleData.getSites(bounds).isEmpty());

	}

	@Test
	public void testGetSites_2Tiles() throws IOException {

		File tileDir =  TestFileUtil.getTestFile("terrarium-xyz");
		assertTrue(tileDir.isDirectory());

		var eleData = new TerrariumXYZData(new TileUriPattern("file://" + tileDir.getAbsolutePath()
				+ File.separator + "{z}" + File.separator + "{x}" + File.separator + "{y}.webp"));

		var testTiles = List.of(new TileNumber(13, 4290, 2868), new TileNumber(13, 4290, 2869));
		var bounds = new LatLonBounds(
				testTiles.get(1).latLonBounds().getCenter().lat, 8.566,
				testTiles.get(0).latLonBounds().getCenter().lat, 8.567);

		var sites = eleData.getSites(bounds);

		assertFalse(sites.isEmpty());
		assertTrue(sites.stream().anyMatch(site -> testTiles.get(0).latLonBounds().contains(site.latLon())));
		assertTrue(sites.stream().anyMatch(site -> testTiles.get(1).latLonBounds().contains(site.latLon())));

	}

}
