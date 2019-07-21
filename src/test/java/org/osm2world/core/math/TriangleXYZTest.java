package org.osm2world.core.math;

import static org.junit.Assert.*;
import static org.osm2world.core.math.VectorXYZ.*;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

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
	public void testIsDegenerate() {

		TriangleXYZ t1 = new TriangleXYZ(
				new VectorXYZ(0, 0, 0),
				new VectorXYZ(0, 1, 0),
				new VectorXYZ(0, 1, 1));

		assertFalse(t1.isDegenerate());

		TriangleXYZ t2 = new TriangleXYZ(
				new VectorXYZ(0, 0, 0),
				new VectorXYZ(0, 0.5, 0),
				new VectorXYZ(0, 1.0, 0));

		assertTrue(t2.isDegenerate());

		TriangleXYZ t3 = new TriangleXYZ(
				new VectorXYZ(0, 0, 0),
				new VectorXYZ(0, 0, 0),
				new VectorXYZ(1, 2, 3));

		assertTrue(t3.isDegenerate());

	}
}
