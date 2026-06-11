package org.osm2world.math.shapes;

import static java.lang.Math.sqrt;
import static org.junit.Assert.*;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import java.util.List;

import org.junit.Test;
import org.osm2world.math.VectorXZ;

public class RectangleXZTest {

	@Test
	public void testGetArea() {

		RectangleXZ rect = new RectangleXZ(
				new VectorXZ(0, 0),
				new VectorXZ(0, -2),
				new VectorXZ(10, -2),
				new VectorXZ(10, 0));

		assertEquals(20, rect.getArea(), 0.001);
		assertEquals(rect.getArea(), rect.polygonXZ().getArea(), 0.001);

	}

	@Test
	public void testGetDiameter() {

		RectangleXZ rect = new RectangleXZ(
				new VectorXZ(0, 0),
				new VectorXZ(0, -2),
				new VectorXZ(10, -2),
				new VectorXZ(10, 0));

		assertEquals(sqrt(100 + 4), rect.getDiameter(), 0.001);
		assertEquals(rect.getDiameter(), rect.polygonXZ().getDiameter(), 0.001);

	}

	@Test
	public void testGetCentroid() {

		RectangleXZ rect = new RectangleXZ(
				new VectorXZ(0, 0),
				new VectorXZ(0, 2),
				new VectorXZ(4, 2),
				new VectorXZ(4, 0));

		assertEquals(new VectorXZ(2, 1), rect.getCentroid());
		assertAlmostEquals(rect.getCentroid(), rect.polygonXZ().getCentroid());

	}

	@Test
	public void testIsClockwise() {

		var clockwiseRect = new RectangleXZ(
				new VectorXZ(0, 0),
				new VectorXZ(0, 2),
				new VectorXZ(4, 2),
				new VectorXZ(4, 0));

		assertTrue(clockwiseRect.isClockwise());
		assertTrue(clockwiseRect.polygonXZ().isClockwise());

		var counterClockwiseRect = new RectangleXZ(
				new VectorXZ(4, 0),
				new VectorXZ(4, 2),
				new VectorXZ(0, 2),
				new VectorXZ(0, 0));

		assertFalse(counterClockwiseRect.isClockwise());
		assertFalse(counterClockwiseRect.polygonXZ().isClockwise());

	}

	@Test
	public void testShift() {

		var rect = new RectangleXZ(
				new VectorXZ(0, 0),
				new VectorXZ(0, 2),
				new VectorXZ(4, 2),
				new VectorXZ(4, 0));

		var shiftVector = new VectorXZ(1, -1);
		var shiftedRect = rect.shift(shiftVector);

		List<VectorXZ> expected = List.of(
				new VectorXZ(1, -1),
				new VectorXZ(1, 1),
				new VectorXZ(5, 1),
				new VectorXZ(5, -1));
		assertEquals(expected, shiftedRect.verticesNoDup());

		assertEquals(rect.getArea(), shiftedRect.getArea(), 0.001);

	}

}
