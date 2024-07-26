package org.osm2world.core.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ResolutionTest {

	@Test
	public void testAspectRatio() {

		var r1 = new Resolution(1000, 500);
		var r2 = new Resolution("256x256");
		var r3 = new Resolution("64,128");

		assertEquals(2.0f, r1.getAspectRatio(), 0f);
		assertEquals(1.0f, r2.getAspectRatio(), 0f);
		assertEquals(0.5f, r3.getAspectRatio(), 0f);

	}

}
