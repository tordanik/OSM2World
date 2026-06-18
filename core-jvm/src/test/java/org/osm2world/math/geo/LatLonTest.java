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

}
