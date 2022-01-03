package org.osm2world.core.math;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.math.VectorXZ.*;
import static org.osm2world.core.test.TestUtil.*;

import java.util.List;

import org.junit.Test;

public class GeometryUtilTest {

	@Test
	public void testIsRightOf() {

		assertTrue(isRightOf(X_UNIT, NULL_VECTOR, Z_UNIT));
		assertFalse(isRightOf(X_UNIT, Z_UNIT, NULL_VECTOR));

		assertTrue(isRightOf(NULL_VECTOR, Z_UNIT, X_UNIT));
		assertFalse(isRightOf(NULL_VECTOR, X_UNIT, Z_UNIT));

		for (VectorXZ v1 : anyVectorXZ()) {
			for (VectorXZ v2 : anyVectorXZ()) {

				if (!v1.equals(v2) && !v2.equals(NULL_VECTOR)) {

					VectorXZ l1 = v1;
					VectorXZ l2 = v1.add(v2);
					VectorXZ pR = v1.add(v2.rightNormal());
					VectorXZ pL = v1.subtract(v2.rightNormal());

					assertTrue(pR + " should be right of " + l1 + "-" + "l2",
							isRightOf(pR, l1, l2));
					assertFalse(pL + " should not be right of " + l1 + "-" + "l2",
							isRightOf(pL, l1, l2));

				}

			}
		}

	}

	@Test
	public void testIsBetween() {

		assertTrue(isBetween(NULL_VECTOR, X_UNIT, X_UNIT.invert()));
		assertTrue(isBetween(NULL_VECTOR, X_UNIT.invert(), X_UNIT));
		assertTrue(isBetween(Z_UNIT, X_UNIT.invert(), X_UNIT));

	}

	@Test
	public void testRoughlyContains() {

		PolygonWithHolesXZ p1 = new PolygonWithHolesXZ(new SimplePolygonXZ(asList(
				new VectorXZ(-1, -1),
				new VectorXZ(+1, -1),
				new VectorXZ(+1, +1),
				new VectorXZ(-1, +1),
				new VectorXZ(-1, -1)
		)), emptyList());

		SimplePolygonXZ p2a = new SimplePolygonXZ(asList(
				new VectorXZ( 0, -1),
				new VectorXZ(+1, -1),
				new VectorXZ(+1, +1.01),
				new VectorXZ( 0, +1),
				new VectorXZ( 0, -1)
		));

		SimplePolygonXZ p2b = new SimplePolygonXZ(asList(
				new VectorXZ( 0, -1),
				new VectorXZ(+1, -1),
				new VectorXZ(+1, +1.5),
				new VectorXZ( 0, +1),
				new VectorXZ( 0, -1)
		));

		assertTrue(roughlyContains(p1, p2a));
		assertFalse(roughlyContains(p1, p2b));

	}

	@Test
	public void testProjectPerpendicular() {

		VectorXZ v1 = new VectorXZ(-10, 2);
		VectorXZ v2 = new VectorXZ(+10, 2);

		assertAlmostEquals(new VectorXZ(5, 2), projectPerpendicular(new VectorXZ(5, 5), v1, v2));
		assertAlmostEquals(new VectorXZ(5, 2), projectPerpendicular(new VectorXZ(5, 5), v2, v1));

		assertAlmostEquals(new VectorXZ(15, 2), projectPerpendicular(new VectorXZ(15, 0), v1, v2));
		assertAlmostEquals(new VectorXZ(15, 2), projectPerpendicular(new VectorXZ(15, 0), v2, v1));

		assertAlmostEquals(new VectorXZ(-3, 2), projectPerpendicular(new VectorXZ(-3, 2), v1, v2));
		assertAlmostEquals(new VectorXZ(-3, 2), projectPerpendicular(new VectorXZ(-3, 2), v2, v1));

	}

	@Test
	public void testInterpolateElevation() {

		assertEquals(9.0, GeometryUtil.interpolateElevation(
				new VectorXZ(5, 1),
				new VectorXYZ(3, 7, 1),
				new VectorXYZ(6, 10, 1)).y,
				1e-5);

	}

	@Test
	public void testInterpolateOn() {

		List<VectorXYZ> linestring = asList(
				new VectorXYZ(-5, 0, 0),
				new VectorXYZ(0, 0, 0),
				new VectorXYZ(0, 3, 0),
				new VectorXYZ(0, 3, 2)
				);

		assertAlmostEquals(-5, 0, 0, interpolateOn(linestring, 0.0));
		assertAlmostEquals(-2, 0, 0, interpolateOn(linestring, 0.3));
		assertAlmostEquals( 0, 0, 0, interpolateOn(linestring, 0.5));
		assertAlmostEquals( 0, 1, 0, interpolateOn(linestring, 0.6));
		assertAlmostEquals( 0, 3, 0, interpolateOn(linestring, 0.8));
		assertAlmostEquals( 0, 3, 1, interpolateOn(linestring, 0.9));
		assertAlmostEquals( 0, 3, 2, interpolateOn(linestring, 1.0));

	}

	@Test
	public void testInterpolateOnTriangle() {

		TriangleXZ t = new TriangleXZ(new VectorXZ(0, 10), new VectorXZ(10, 0), new VectorXZ(20, 10));

		assertEquals(1, interpolateOnTriangle(t.v1, t, 1, -100, 3), 0.01);
		assertEquals(-100, interpolateOnTriangle(t.v2, t, 1, -100, 3), 0.01);
		assertEquals(3, interpolateOnTriangle(t.v3, t, 1, -100, 3), 0.01);

		assertEquals(2.0, interpolateOnTriangle(new VectorXZ(10, 10), t, 1, -100, 3), 0.01);

	}

	@Test
	public void testEquallyDistributePointsAlong1StartEnd() {

		List<VectorXZ> result1 = equallyDistributePointsAlong(
				1f, true, new VectorXZ(-2, 5), new VectorXZ(+4, 5));
		List<VectorXYZ> result2 = equallyDistributePointsAlong(
				1f, true, asList(new VectorXYZ(-2, 0, 5), new VectorXYZ(+4, 2, 5)));

		for (List<? extends Vector3D> result : asList(result1, result2)) {

			assertSame(7, result.size());
			assertAlmostEquals(-2, 5, result.get(0).xz());
			assertAlmostEquals(-1, 5, result.get(1).xz());
			assertAlmostEquals( 0, 5, result.get(2).xz());
			assertAlmostEquals(+1, 5, result.get(3).xz());
			assertAlmostEquals(+2, 5, result.get(4).xz());
			assertAlmostEquals(+3, 5, result.get(5).xz());
			assertAlmostEquals(+4, 5, result.get(6).xz());

		}

	}

	@Test
	public void testEquallyDistributePointsAlong1NoStartEnd() {

		List<VectorXZ> result1 = equallyDistributePointsAlong(
				1f, false, new VectorXZ(-2, 5), new VectorXZ(+4, 5));
		List<VectorXYZ> result2 = equallyDistributePointsAlong(
				1f, false, asList(new VectorXYZ(-2, 0, 5), new VectorXYZ(+4, 2, 5)));

		for (List<? extends Vector3D> result : asList(result1, result2)) {

			assertSame(6, result.size());
			assertAlmostEquals(-1.5f, 5, result.get(0).xz());
			assertAlmostEquals(-0.5f, 5, result.get(1).xz());
			assertAlmostEquals(+0.5f, 5, result.get(2).xz());
			assertAlmostEquals(+1.5f, 5, result.get(3).xz());
			assertAlmostEquals(+2.5f, 5, result.get(4).xz());
			assertAlmostEquals(+3.5f, 5, result.get(5).xz());

		}

	}

	@Test
	public void testEquallyDistributePointsAlong2SelfIntersect() {

		List<VectorXYZ> input = asList(
				new VectorXYZ( 0, 0, -1),
				new VectorXYZ( 0, 0, +1),
				new VectorXYZ(+1, 2, +1),
				new VectorXYZ(+1, 7,  0),
				new VectorXYZ(-1, 9,  0));

		List<VectorXYZ> result = equallyDistributePointsAlong(1f, true, input);

		assertSame(7, result.size());
		assertAlmostEquals( 0, 0, -1, result.get(0));
		assertAlmostEquals( 0, 0,  0, result.get(1));
		assertAlmostEquals( 0, 0, +1, result.get(2));
		assertAlmostEquals(+1, 2, +1, result.get(3));
		assertAlmostEquals(+1, 7,  0, result.get(4));
		assertAlmostEquals( 0, 8,  0, result.get(5));
		assertAlmostEquals(-1, 9,  0, result.get(6));

	}

	/**
	 * regression test, merely checks for absence of exceptions
	 * triggered by floating point inaccuracies
	 */
	@Test
	public void testEquallyDistributePointsAlong3FloatingPoint() {

		equallyDistributePointsAlong(
				0.12f, true, asList(new VectorXYZ(0, 0, 0), new VectorXYZ(100, 0, 0)));

	}

}
