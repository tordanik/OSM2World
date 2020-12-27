package org.osm2world.core.target.common.rendering;

import static org.junit.Assert.assertEquals;

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
		TileNumber parsedTileNumber = new TileNumber("13,4402,2828");
		assertEquals(tileNumber, parsedTileNumber);
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

}
