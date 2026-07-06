package org.osm2world.math.geo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LatLonTest {

	@Test
	public void testDistanceTo() {

		assertEquals(0, new LatLon(42, -42).distanceTo(new LatLon(42, -42)), 0.001);

		var eiffelTower = new LatLon(48.858, 2.294);
		var whiteHouse = new LatLon(38.898, -77.037);
		double distance = eiffelTower.distanceTo(whiteHouse);
		assertEquals(6161600, distance, 1000);
		assertEquals(distance, whiteHouse.distanceTo(eiffelTower), 0.1);

	}

	@Test
	public void testConstructorFromString() {

		String s0 = "48.56704,13.44887";
		assertEquals(new LatLon(48.56704, 13.44887), new LatLon(s0));

		String s1 = "-22.3,-10.9";
		assertEquals(new LatLon(-22.3, -10.9), new LatLon(s1));

	}

}
