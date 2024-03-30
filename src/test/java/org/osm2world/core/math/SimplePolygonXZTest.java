package org.osm2world.core.math;

import static java.lang.Math.sqrt;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.osm2world.core.math.PolygonUtils.isSelfIntersecting;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.test.TestUtil.*;

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
	public void testClosestPoint() {

		// within polygon
		assertAlmostEquals(new VectorXZ(0.2, 0.2), p1.closestPoint(new VectorXZ(0.2, 0.2)));
		assertAlmostEquals(new VectorXZ(-1, -1), p1.closestPoint(new VectorXZ(-1, -1)));

		// outside polygon
		assertAlmostEquals(new VectorXZ(-1, -1), p1.closestPoint(new VectorXZ(-10, -10)));
		assertAlmostEquals(new VectorXZ(1, 0.5), p1.closestPoint(new VectorXZ(26, 0.5)));

	}

	@Test
	public void testShift() {

		SimplePolygonShapeXZ shiftP = p1.shift(VectorXZ.X_UNIT);

		assertSame(p1.size(), shiftP.vertices().size() - 1);
		assertAlmostEquals(new VectorXZ( 0, -1), shiftP.vertices().get(0));
		assertAlmostEquals(new VectorXZ( 0,  0), shiftP.vertices().get(1));
		assertAlmostEquals(new VectorXZ( 0, +1), shiftP.vertices().get(2));
		assertAlmostEquals(new VectorXZ( 2, +1), shiftP.vertices().get(3));
		assertAlmostEquals(new VectorXZ( 2, -1), shiftP.vertices().get(4));
		assertAlmostEquals(new VectorXZ( 0, -1), shiftP.vertices().get(5));

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

	@Test
	public void testConvexHull() {

		SimplePolygonXZ p = new SimplePolygonXZ(asList(
				new VectorXZ( -5, 0),
				new VectorXZ( -4, 1),
				new VectorXZ( -3, 3),
				new VectorXZ( -2, 2),
				new VectorXZ( -1, 1),
				new VectorXZ(  0, 4),
				new VectorXZ( +1, 4),
				new VectorXZ( +2, 1),
				new VectorXZ( +3, 2),
				new VectorXZ( +4, 2),
				new VectorXZ( +5, 0),
				new VectorXZ( -5, 0)));

		SimplePolygonXZ hull = p.convexHull();

		assertSameCyclicOrder(false, hull.getVertices(),
				new VectorXZ( -5, 0),
				new VectorXZ( -3, 3),
				new VectorXZ(  0, 4),
				new VectorXZ( +1, 4),
				new VectorXZ( +4, 2),
				new VectorXZ( +5, 0));

	}

	@Test
	public void testConvexHull2() {

		SimplePolygonXZ p = new SimplePolygonXZ(asList(
				new VectorXZ( -2.0,  0.0),
				new VectorXZ( -0.5, -0.5),
				new VectorXZ(  0.0, -2.0),
				new VectorXZ( +0.5, -0.5),
				new VectorXZ( +2.0,  0.0),
				new VectorXZ(  0.0, -0.5),
				new VectorXZ( -2.0,  0.0)));

		SimplePolygonXZ hull = p.convexHull();

		assertSameCyclicOrder(false, hull.getVertices(),
				new VectorXZ( -2.0,  0.0),
				new VectorXZ(  0.0, -2.0),
				new VectorXZ( +2.0,  0.0));

	}

	@Test
	public void testConvexHull3() {

		SimplePolygonXZ p = new SimplePolygonXZ(asList(
				new VectorXZ(-1, 0),
				new VectorXZ(+1, 0),
				new VectorXZ( 0, 1),
				new VectorXZ(-1, 0)));

		SimplePolygonXZ hull = p.convexHull();

		assertSameCyclicOrder(false, hull.getVertices(),
				new VectorXZ(-1, 0),
				new VectorXZ(+1, 0),
				new VectorXZ( 0, 1));

	}

	@Test
	public void testConvexHull4() {

		SimplePolygonXZ p = new SimplePolygonXZ(asList(
				new VectorXZ(0, 0),
				new VectorXZ(0, 2),
				new VectorXZ(1, 1),
				new VectorXZ(2, 2),
				new VectorXZ(2, 0),
				new VectorXZ(0, 0)));

		SimplePolygonXZ hull = p.convexHull();

		assertSameCyclicOrder(false, hull.getVertices(),
				new VectorXZ(0, 0),
				new VectorXZ(0, 2),
				new VectorXZ(2, 2),
				new VectorXZ(2, 0));

	}

	/** simple test cases where the bounding box is identical to the polygon */
	@Test
	public void testMinimumBoundingBox() {

		//axis aligned case

		SimplePolygonXZ p1 = new SimplePolygonXZ(asList(
				new VectorXZ(0, 0),
				new VectorXZ(1, 0),
				new VectorXZ(1, 1),
				new VectorXZ(0, 1),
				new VectorXZ(0, 0)));

		SimplePolygonXZ bbox1 = p1.minimumRotatedBoundingBox();

		assertSameCyclicOrder(true, bbox1.getVertices(),
				new VectorXZ(0, 0),
				new VectorXZ(1, 0),
				new VectorXZ(1, 1),
				new VectorXZ(0, 1));

		//not aligned to axis

		SimplePolygonXZ p2 = new SimplePolygonXZ(asList(
				new VectorXZ(+.5,   0),
				new VectorXZ(  0, +.5),
				new VectorXZ(-.5,   0),
				new VectorXZ(  0, -.5),
				new VectorXZ(+.5,   0)));

		SimplePolygonXZ bbox2 = p2.minimumRotatedBoundingBox();

		assertSameCyclicOrder(true, bbox2.getVertices(),
				new VectorXZ(+.5,   0),
				new VectorXZ(  0, +.5),
				new VectorXZ(-.5,   0),
				new VectorXZ(  0, -.5));

	}

	@Test
	public void testMinimumBoundingBox2() {

		SimplePolygonXZ p = new SimplePolygonXZ(asList(
				new VectorXZ(1, 1),
				new VectorXZ(2, 1),
				new VectorXZ(4, 3),
				new VectorXZ(3, 3),
				new VectorXZ(1, 1)));

		SimplePolygonXZ bbox = p.minimumRotatedBoundingBox();

		assertSameCyclicOrder(true, bbox.getVertices(),
				new VectorXZ(1, 1),
				new VectorXZ(3.5, 3.5),
				new VectorXZ(4, 3),
				new VectorXZ(1.5, 0.5));

	}

}
