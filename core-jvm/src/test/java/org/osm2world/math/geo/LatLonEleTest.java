package org.osm2world.math.geo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LatLonEleTest {

	@Test
	public void testConstructorFromString() {

		String s0 = "48.56704,13.44887,4.0";
		assertEquals(new LatLonEle(48.56704, 13.44887, 4.0), new LatLonEle(s0));

		String s1 = "-22.3,-10.9,-3.0";
		assertEquals(new LatLonEle(-22.3, -10.9, -3.0), new LatLonEle(s1));

		String s2 = "12,34";
		assertEquals(new LatLonEle(12, 34, 0), new LatLonEle(s2));

	}

}
