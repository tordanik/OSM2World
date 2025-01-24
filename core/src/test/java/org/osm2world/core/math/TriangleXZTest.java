package org.osm2world.core.math;

import static org.junit.Assert.*;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import org.junit.Test;

public class TriangleXZTest {

	private final TriangleXZ triangleCW = new TriangleXZ(
			new VectorXZ( 0, 0),
			new VectorXZ(-1, 1),
			new VectorXZ(+1, 0));

	private final TriangleXZ triangleCCW = new TriangleXZ(
			new VectorXZ( 0, 0),
			new VectorXZ(-1, 1),
			new VectorXZ(-2, 0));

	@Test
	public void testIsClockwise() {
		assertTrue(triangleCW.isClockwise());
		assertFalse(triangleCW.reverse().isClockwise());
		assertFalse(triangleCCW.isClockwise());
		assertTrue(triangleCCW.reverse().isClockwise());
	}

	@Test
	public void testGetArea() {
		assertAlmostEquals(triangleCW.getArea(), 0.5);
		assertAlmostEquals(triangleCCW.getArea(), 1);
	}

}
