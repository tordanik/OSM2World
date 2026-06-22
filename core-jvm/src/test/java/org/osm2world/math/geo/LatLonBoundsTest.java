package org.osm2world.math.geo;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class LatLonBoundsTest {

	@Test
	public void testIntersection() {

		var b1 = new LatLonBounds(0, 0, 10, 10);
		var b2 = new LatLonBounds(5, 5, 14, 15);

		var expected1 = new LatLonBounds(5, 5, 10, 10);
		assertEquals(expected1, LatLonBounds.intersection(List.of(b1, b2)));

		var b3 = new LatLonBounds(12, 13, 30, 30);

		var expected2 = new LatLonBounds(12, 13, 14, 15);
		assertEquals(expected2, LatLonBounds.intersection(List.of(b2, b3)));

		assertNull(LatLonBounds.intersection(List.of(b1, b3)));
		assertNull(LatLonBounds.intersection(List.of(b1, b2, b3)));

	}

	@Test
	public void testContains() {

		var b1 = new LatLonBounds(0, 0, 10, 10);
		var b2 = new LatLonBounds(0, 1, 8, 9);

		assertTrue(b1.contains(b1));
		assertTrue(b2.contains(b2));

		assertTrue(b1.contains(b2));
		assertFalse(b2.contains(b1));

	}

}
