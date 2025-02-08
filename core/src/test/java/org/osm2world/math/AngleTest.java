package org.osm2world.math;

import static java.lang.Math.PI;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AngleTest {

	@Test
	public void testFitToRange() {
		assertEquals(0, Angle.ofRadians(0).radians, 0);
		assertEquals(PI, Angle.ofRadians(PI).radians, 0);
		assertEquals(0, Angle.ofRadians(2 * PI).radians, 0);
		assertEquals(PI, Angle.ofRadians(3 * PI).radians, 0);
		assertEquals(1.5 * PI, Angle.ofRadians(-PI / 2).radians, 0);
	}

	@Test
	public void testOfDegrees() {
		assertEquals(0, Angle.ofDegrees(0).radians, 0);
		assertEquals(PI, Angle.ofDegrees(180).radians, 0);
		assertEquals(0, Angle.ofDegrees(360).radians, 0);
		assertEquals(PI, Angle.ofDegrees(540).radians, 0);
		assertEquals(1.5 * PI, Angle.ofDegrees(-90).radians, 0);
	}

}
