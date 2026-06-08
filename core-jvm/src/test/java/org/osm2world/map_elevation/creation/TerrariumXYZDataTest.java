package org.osm2world.map_elevation.creation;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.math.geo.LatLonEle;
import org.osm2world.math.geo.TileNumber;
import org.osm2world.scene.color.Color;
import org.osm2world.util.platform.image.ImageImplementationJvm;
import org.osm2world.util.platform.uri.HttpUriImplementationJvm;
import org.osm2world.util.test.TestFileUtil;
import org.osm2world.util.tiles.TileUriPattern;

public class TerrariumXYZDataTest {

	static {
		HttpUriImplementationJvm.register();
		ImageImplementationJvm.register();
	}

	@Test
	public void testDecodeValue() {
		assertEquals(2523.265625, TerrariumXYZData.decodeValue(new Color(137, 219, 68).getRGB()), 0.0);
	}

	@Test
	public void testEncodeValue() {
		assertEquals(new Color(137, 219, 68).getRGB(), TerrariumXYZData.encodeValue(2523.265625));
	}

	@Test
	public void testEncodeDecode() {
		for (Color c : List.of(Color.WHITE, Color.BLACK, Color.RED, Color.YELLOW, Color.PINK)) {
			assertEquals(c.getRGB(), TerrariumXYZData.encodeValue(TerrariumXYZData.decodeValue(c.getRGB())));
		}
	}

	@Test
	public void testGetSites() throws IOException {

		var eleData = eleDataFromTestResources(13, "webp");

		var bounds = new LatLonBounds(47.385, 8.566, 47.386, 8.567);
		assertFalse(eleData.getSites(bounds).isEmpty());

	}

	@Test
	public void testGetSites_2Tiles() throws IOException {

		var eleData = eleDataFromTestResources(13, "webp");

		var testTiles = List.of(new TileNumber(13, 4290, 2868), new TileNumber(13, 4290, 2869));
		var bounds = new LatLonBounds(
				testTiles.get(1).latLonBounds().getCenter().lat, 8.566,
				testTiles.get(0).latLonBounds().getCenter().lat, 8.567);

		var sites = eleData.getSites(bounds);

		assertFalse(sites.isEmpty());
		assertTrue(sites.stream().anyMatch(site -> testTiles.get(0).latLonBounds().contains(site.latLon())));
		assertTrue(sites.stream().anyMatch(site -> testTiles.get(1).latLonBounds().contains(site.latLon())));

	}

	/**
	 * tests a situation where the requested tile number itself does not exist, so data is pulled from an ancestor tile
	 */
	@Test
	public void testGetSites_ancestorTile() throws IOException {

		var eleData = eleDataFromTestResources(6, "png");

		for (var tileColor : List.of(
				Pair.of(new TileNumber(6, 2, 1), 42.0),
				Pair.of(new TileNumber(6, 13, 15), 1337.0))) {

			TileNumber tile = tileColor.getLeft();
			LatLonBounds bounds = tile.latLonBounds().pad(-0.1);
			Collection<LatLonEle> sites = eleData.getSites(bounds);
			assertEquals(4, sites.size());
			sites.forEach(site -> assertEquals(tileColor.getRight(), site.ele, 0.0));

		}

	}

	@Nonnull
	private static TerrariumXYZData eleDataFromTestResources(Integer maxZoom, String fileExt) {

		File tileDir = TestFileUtil.getTestFile("terrarium-xyz");
		assertTrue(tileDir.isDirectory());

		return new TerrariumXYZData(maxZoom, new TileUriPattern("file://" + tileDir.getAbsolutePath()
				+ File.separator + "{z}" + File.separator + "{x}" + File.separator + "{y}." + fileExt));

	}

}
