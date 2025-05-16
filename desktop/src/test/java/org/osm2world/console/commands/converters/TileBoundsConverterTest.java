package org.osm2world.console.commands.converters;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;
import org.osm2world.math.geo.TileNumber;

public class TileBoundsConverterTest {

	@Test
	public void testConvert() {

		var converter = new TileBoundsConverter();

		assertEquals(Set.of(new TileNumber(13, 246, 4001)),
				converter.convert("13,246,4001").getTiles());
		assertEquals(Set.of(new TileNumber(12, 0, 4001)),
				converter.convert("12/0/4001").getTiles());
		assertEquals(Set.of(new TileNumber(11, 246, 0)),
				converter.convert("11_246_0").getTiles());

	}

}