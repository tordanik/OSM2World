package org.osm2world.math.geo;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.Test;

public class TileBoundsTest {

	@Test
	public void testAround() {

		var r1 = TileBounds.around(List.of(new TileNumber(13, 100, 100)));
		assertEquals(Set.of(new TileNumber(13, 100, 100)), r1.getTiles());

		var r2 = TileBounds.around(List.of(new TileNumber(1, 0, 0), new TileNumber(2, 2, 2)));
		assertEquals(2, r2.getTiles().iterator().next().zoom);
		assertEquals(9, r2.getTiles().size());

		var r3 = TileBounds.around(List.of(new TileNumber(1, 0, 0), new TileNumber(2, 1, 1)), 1);
		assertEquals(Set.of(new TileNumber(1, 0, 0)), r3.getTiles());


	}

}