package org.osm2world.core.math;

import static java.lang.Math.sqrt;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.osm2world.core.math.SimplePolygonXZ.isSelfIntersecting;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.osm2world.core.math.shapes.SimplePolygonShapeXZ;

public class SimplePolygonXZTest {

	private SimplePolygonXZ p1 = new SimplePolygonXZ(asList(
			new VectorXZ(-1, -1),
			new VectorXZ(-1,  0),
			new VectorXZ(-1, +1),
			new VectorXZ(+1, +1),
			new VectorXZ(+1, -1),
			new VectorXZ(-1, -1)));

	private SimplePolygonXZ p2 = new SimplePolygonXZ(asList(
			new VectorXZ(-0.5, -0.5),
			new VectorXZ(-0.5, +1.5),
			new VectorXZ(+1.5, +1.5),
			new VectorXZ(+1.5, -0.5),
			new VectorXZ(-0.5, -0.5)));

	@Test
	public void testGetCentroid() {

		assertAlmostEquals(NULL_VECTOR, p1.getCentroid());
		assertAlmostEquals(NULL_VECTOR, p1.reverse().getCentroid());

		assertAlmostEquals(new VectorXZ(0.5, 0.5), p2.getCentroid());
		assertAlmostEquals(new VectorXZ(0.5, 0.5), p2.reverse().getCentroid());

	}

	@Test
	public void testGetArea() {

		assertAlmostEquals(4, p1.getArea());
		assertAlmostEquals(4, p1.reverse().getArea());

		assertAlmostEquals(4, p2.getArea());
		assertAlmostEquals(4, p2.reverse().getArea());

	}

	@Test
	public void testGetDiameter() {

		assertAlmostEquals(sqrt(8), p1.getDiameter());
		assertAlmostEquals(sqrt(8), p2.getDiameter());

	}

	@Test
	public void testGetSimplifiedPolygon() {

		assertEquals(p2, p2.getSimplifiedPolygon());

		assertEquals(4, p1.getSimplifiedPolygon().size());
		assertAlmostEquals(p1.getArea(), p1.getSimplifiedPolygon().getArea());

	}

	@Test
	public void testDistanceToSegments() {

		assertAlmostEquals(1, p1.distanceToSegments(NULL_VECTOR));

		assertAlmostEquals(0.5, p2.distanceToSegments(NULL_VECTOR));

	}

	@Test
	public void testShift() {

		SimplePolygonShapeXZ shiftP = p1.shift(VectorXZ.X_UNIT);

		assertSame(p1.size(), shiftP.getVertexList().size() - 1);
		assertAlmostEquals(new VectorXZ( 0, -1), shiftP.getVertexList().get(0));
		assertAlmostEquals(new VectorXZ( 0,  0), shiftP.getVertexList().get(1));
		assertAlmostEquals(new VectorXZ( 0, +1), shiftP.getVertexList().get(2));
		assertAlmostEquals(new VectorXZ( 2, +1), shiftP.getVertexList().get(3));
		assertAlmostEquals(new VectorXZ( 2, -1), shiftP.getVertexList().get(4));
		assertAlmostEquals(new VectorXZ( 0, -1), shiftP.getVertexList().get(5));

	}

	private static final VectorXZ outlineA0 = new VectorXZ(-1.1f, -1.1f);
	private static final VectorXZ outlineA1 = new VectorXZ(-1.1f, 1.1f);
	private static final VectorXZ outlineA2 = new VectorXZ(1.1f, 1.1f);
	private static final VectorXZ outlineA3 = new VectorXZ(1.1f, -1.1f);
	private static final VectorXZ outlineA4 = new VectorXZ(0f, 1f);

	private static final List<VectorXZ> outlineA = Arrays.asList(outlineA0,
			outlineA1, outlineA2, outlineA3, outlineA0);

	private static final List<VectorXZ> outlineB = Arrays.asList(outlineA0,
			outlineA1, outlineA2, outlineA3, outlineA4, outlineA0);

	@Test
	public void testIsClockwise1() {

		assertTrue(new SimplePolygonXZ(outlineA).isClockwise());

		List<VectorXZ> outlineAInv = new ArrayList<VectorXZ>(outlineA);
		Collections.reverse(outlineAInv);
		assertFalse(new SimplePolygonXZ(outlineAInv).isClockwise());

	}

	@Test
	public void testIsClockwise2() {

		assertTrue(new SimplePolygonXZ(outlineB).isClockwise());

		List<VectorXZ> outlineBInv = new ArrayList<VectorXZ>(outlineB);
		Collections.reverse(outlineBInv);
		assertFalse(new SimplePolygonXZ(outlineBInv).isClockwise());
	}

	@Test
	public void testIsClockwise3() {

		// test case created from a former bug
		assertTrue(new SimplePolygonXZ(Arrays.asList(
				new VectorXZ(114266.61f,12953.262f),
				new VectorXZ(114258.74f,12933.117f),
				new VectorXZ(114257.69f,12939.848f),
				new VectorXZ(114266.61f,12953.262f))).isClockwise());


	}

	@Test
	public void testIsEquivalentTo_same() {

		SimplePolygonXZ polyA = new SimplePolygonXZ(outlineA);
		assertTrue(polyA.isEquivalentTo(polyA));

	}

	@Test
	public void testIsEquivalentTo_yes() {

		assertTrue(new SimplePolygonXZ(Arrays.asList(outlineA0, outlineA1, outlineA4, outlineA0)).isEquivalentTo(
				new SimplePolygonXZ(Arrays.asList(outlineA1, outlineA4, outlineA0, outlineA1))));

	}

	@Test
	public void testIsEquivalentTo_no() {

		assertFalse(new SimplePolygonXZ(Arrays.asList(outlineA0, outlineA1, outlineA4, outlineA0)).isEquivalentTo(
				new SimplePolygonXZ(Arrays.asList(outlineA0, outlineA4, outlineA1, outlineA0))));

	}

	@Test
	public void testIsSelfIntersecting1() {

		VectorXZ v1 = new VectorXZ(0, 0);
		VectorXZ v2 = new VectorXZ(1, 0);
		VectorXZ v3 = new VectorXZ(1, 1);
		VectorXZ v4 = new VectorXZ(0, 1);

		// Simple rectangle should not be self intersecting
		assertFalse(isSelfIntersecting(Arrays.asList(v1, v2, v3, v4, v1)));

	}

	@Test
	public void testIsSelfIntersecting2() {

		VectorXZ v1 = new VectorXZ(1, 5);
		VectorXZ v2 = new VectorXZ(5, 2);
		VectorXZ v3 = new VectorXZ(4, 1);
		VectorXZ v4a = new VectorXZ(3, 4);
		VectorXZ v4b = new VectorXZ(3, 5);
		VectorXZ v4c = new VectorXZ(3, 6);

		/*
		 * testing a simple self intersecting polygon with 4 vertices
		 * while the line causing the intersection is above, below or at the
		 * same height as the beginning node
		 */

		assertTrue(isSelfIntersecting(Arrays.asList(v1, v2, v3, v4a, v1)));
		assertTrue(isSelfIntersecting(Arrays.asList(v1, v2, v3, v4b, v1)));
		assertTrue(isSelfIntersecting(Arrays.asList(v1, v2, v3, v4c, v1)));

	}

	@Test
	public void testIsSelfIntersecting3() {

		VectorXZ v1 = new VectorXZ(0, 0);

		VectorXZ v2 = new VectorXZ(10, 0);
		VectorXZ v3 = new VectorXZ(15, 0);
		VectorXZ v4 = new VectorXZ(20, 0);

		VectorXZ v5 = new VectorXZ(20, 20);
		VectorXZ v6 = new VectorXZ(0, 20);

		// Testing simple non self intersecting polygons

		// ... with one vertex is used two times
		assertFalse(isSelfIntersecting(Arrays.asList(v1, v2, v2, v5, v6, v1)));

		// ... with 3 parallel line segments
		assertFalse(isSelfIntersecting(Arrays.asList(v1, v2, v3, v4, v5, v6, v1)));

		// ... with 3 co-linear line segments
		assertFalse(isSelfIntersecting(Arrays.asList(v1, v2, v4, v3, v4, v5, v6, v1)));

	}

	@Test
	public void testIsSelfIntersecting4() {

		VectorXZ v1 = new VectorXZ(0, 0);
		VectorXZ v2 = new VectorXZ(0, 1);

		VectorXZ v3 = new VectorXZ(0, 3);
		VectorXZ v4 = new VectorXZ(0, 4);

		VectorXZ v5 = new VectorXZ(2, 2);
		VectorXZ v6 = new VectorXZ(4, 2);

		// Testing simple polygons with two vertical line segments

		// .. without any intersection
		assertFalse(isSelfIntersecting(Arrays.asList(v1, v2, v5, v3, v4, v6, v1)));

		// ... with one point being visited twice
		assertFalse(isSelfIntersecting(Arrays.asList(v1, v2, v6, v3, v4, v6, v1)));

		// .. with a intersection at the right side
		assertTrue(isSelfIntersecting(Arrays.asList(v1, v2, v6, v3, v4, v5, v1)));

	}

}
