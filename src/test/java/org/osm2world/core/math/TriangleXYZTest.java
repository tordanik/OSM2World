package org.osm2world.core.math;

import static org.junit.Assert.assertEquals;
import static org.osm2world.core.math.VectorXYZ.*;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import java.util.List;

import org.junit.Test;


public class TriangleXYZTest {

	@Test
	public void testGetYAt() {

		TriangleXYZ t1 = new TriangleXYZ(X_UNIT, Z_UNIT, Y_UNIT);

		assertAlmostEquals(1, t1.getYAt(new VectorXZ(0, 0)));

		assertAlmostEquals(0, t1.getYAt(new VectorXZ(0, 1)));
		assertAlmostEquals(0, t1.getYAt(new VectorXZ(1, 0)));
		assertAlmostEquals(0, t1.getYAt(new VectorXZ(0.5, 0.5)));
		assertAlmostEquals(0, t1.getYAt(new VectorXZ(0.8, 0.2)));

		assertAlmostEquals(0.5, t1.getYAt(new VectorXZ(0, 0.5)));
		assertAlmostEquals(0.5, t1.getYAt(new VectorXZ(0.5, 0)));
		assertAlmostEquals(0.5, t1.getYAt(new VectorXZ(0.25, 0.25)));

	}

	@Test
	public void testGetArea() {

		TriangleXYZ t1 = new TriangleXYZ(
				new VectorXYZ(0, 0, 0),
				new VectorXYZ(0, 1, 0),
				new VectorXYZ(0, 1, 1));

		assertAlmostEquals(0.5, t1.getArea());

	}

	@Test
	public void testConstructor_valid() {
		new TriangleXYZ(
				new VectorXYZ(0, 0, 0),
				new VectorXYZ(0, 1, 0),
				new VectorXYZ(0, 1, 1));
	}

	@Test(expected = InvalidGeometryException.class)
	public void testConstructor_degenerate0() {
		new TriangleXYZ(
				new VectorXYZ(0, 0, 0),
				new VectorXYZ(0, 0.5, 0),
				new VectorXYZ(0, 1.0, 0));

	}

	@Test(expected = InvalidGeometryException.class)
	public void testConstructor_degenerate1() {
		new TriangleXYZ(
				new VectorXYZ(0, 0, 0),
				new VectorXYZ(0, 0, 0),
				new VectorXYZ(1, 2, 3));

	}

	@Test
	public void testSplitTriangleOnLine() {

		var l = new LineXZ(new VectorXZ(0, -10), new VectorXZ(0, +10));

		var t0 = new TriangleXYZ(new VectorXYZ(-2, 0, 0), new VectorXYZ(-1, 0, 0), new VectorXYZ(-1.5, 1, 0));
		assertEquals(List.of(t0), t0.split(l).stream().toList());

		var t1 = new TriangleXYZ(new VectorXYZ(-1, 0, 0), new VectorXYZ(2, 0, 0), new VectorXYZ(0.5, 1, 0));
		var result1 = t1.split(l);
		assertEquals(3, result1.size());

		var t2 = t1.shift(new VectorXYZ(0, 42, 0));
		var result2 = t2.split(l);
		assertEquals(3, result2.size());

		var t3 = new TriangleXYZ(new VectorXYZ(-1, 0, 0), new VectorXYZ(+1, 0, 0), new VectorXYZ(0, -1, 0));
		var result3 = t3.split(l);
		assertEquals(2, result3.size());

		var t4 = new TriangleXYZ(new VectorXYZ(-1, 0, 0), new VectorXYZ(+1, 0, 0), new VectorXYZ(+1, 10, 0));
		var result4 = t4.split(l);
		assertEquals(3, result4.size());

	}

}
