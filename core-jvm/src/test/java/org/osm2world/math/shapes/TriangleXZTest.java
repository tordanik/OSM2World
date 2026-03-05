package org.osm2world.math.shapes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import org.junit.Test;
import org.osm2world.math.VectorXZ;

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
		assertAlmostEquals(0.5, triangleCW.getArea());
		assertAlmostEquals(1, triangleCCW.getArea());
	}

	@Test
	public void testPolygonXZ() {
		assertTrue(triangleCW.polygonXZ().isClockwise());
		assertAlmostEquals(triangleCW.getArea(), triangleCW.polygonXZ().getArea());
		assertFalse(triangleCCW.polygonXZ().isClockwise());
		assertAlmostEquals(triangleCCW.getArea(), triangleCCW.polygonXZ().getArea());
	}

}
