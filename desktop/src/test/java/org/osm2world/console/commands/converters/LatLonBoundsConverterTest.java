package org.osm2world.console.commands.converters;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.osm2world.math.geo.LatLonBounds;

public class LatLonBoundsConverterTest {

	@Test
	public void testConvert() {

		var converter = new LatLonBoundsConverter();

		assertEquals(new LatLonBounds(33, 10, 34, 10.5),
				converter.convert("33,10  34,10.5"));

		assertEquals(new LatLonBounds(-10, 5, 10, 6),
				converter.convert("10,6  -10,5"));

	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvert_singleCoord() {
		new LatLonBoundsConverter().convert("12.3, 45.6");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvert_characters() {
		new LatLonBoundsConverter().convert("random text");
	}

}