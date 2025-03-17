package org.osm2world.math.geo;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.osm2world.math.geo.TileNumber.tilesForBounds;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import java.util.HashSet;
import java.util.List;

import org.junit.Test;

public class TileNumberTest {

	@Test
	public void testValidTileNumber() {
		TileNumber tileNumber = new TileNumber(2, 0, 3);
		assertEquals(2, tileNumber.zoom);
		assertEquals(0, tileNumber.x);
		assertEquals(3, tileNumber.y);
		assertEquals(0, tileNumber.flippedY());
	}

	@Test
	public void testTileNumberParsing() {
		TileNumber tileNumber = new TileNumber(13, 4402, 2828);
		assertEquals(tileNumber, new TileNumber("13,4402,2828"));
		assertEquals(tileNumber, new TileNumber("13/4402/2828"));
		assertEquals(tileNumber, new TileNumber("13_4402_2828"));
	}

	@Test
	public void testToStringParsing() {
		TileNumber tileNumber = new TileNumber(18, 12345, 67890);
		TileNumber parsedTileNumber = new TileNumber(tileNumber.toString());
		assertEquals(tileNumber, parsedTileNumber);
	}

	@Test
	public void testZoom0() {
		new TileNumber(0, 0, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNegativeZoom() {
		new TileNumber(-5, 0, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNegativeX() {
		new TileNumber(13, -36, 239);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNegativeY() {
		new TileNumber(13, 2828, -239);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTooLargeTileCoord() {
		new TileNumber(2, 4, 4);
	}

	@Test
	public void testAncestor() {

		var t00 = new TileNumber(20, 0, 0);
		for (int zoom = 0; zoom <= t00.zoom; zoom++) {
			assertEquals(new TileNumber(zoom, 0, 0), t00.ancestor(zoom));
		}

		assertEquals(new TileNumber(13, 4402, 2828),
				new TileNumber(15, 17608, 11312).ancestor(13));

	}

	@Test
	public void testLatLonBoundsZ0() {
		LatLonBounds bounds = new TileNumber(0, 0, 0).latLonBounds();
		assertAlmostEquals(-180, bounds.minlon);
		assertAlmostEquals(-85.0511, bounds.minlat);
		assertAlmostEquals(+180, bounds.maxlon);
		assertAlmostEquals(+85.0511, bounds.maxlat);
	}

	@Test
	public void testLatLonBoundsZ2() {
		LatLonBounds bounds = new TileNumber(2, 3, 3).latLonBounds();
		assertAlmostEquals(+90, bounds.minlon);
		assertAlmostEquals(-85.0511, bounds.minlat);
		assertAlmostEquals(+180, bounds.maxlon);
		assertAlmostEquals(-66.5132, bounds.maxlat);
	}

	@Test
	public void testAtLatLon() {
		assertEquals(new TileNumber(13, 4402, 2828), TileNumber.atLatLon(13, new LatLon(48.56687, 13.45127)));
	}

	@Test
	public void testTilesForBounds() {

		assertEquals(List.of(new TileNumber(13, 4402, 2828)),
				tilesForBounds(13, new LatLonBounds(48.56687, 13.45127, 48.56687, 13.45127)));

		assertEquals(new HashSet<>(asList(new TileNumber(13, 4401, 2827), new TileNumber(13, 4401, 2828),
				new TileNumber(13, 4402, 2827), new TileNumber(13, 4402, 2828))),
				new HashSet<>(tilesForBounds(13, new LatLonBounds(48.56687, 13.41368, 48.57982, 13.45127))));

	}

}
