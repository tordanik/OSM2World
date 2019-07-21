package org.osm2world.core.math;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class PolygonXZTest {

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

		PolygonXZ polyA = new PolygonXZ(outlineA);
		assertTrue(polyA.isEquivalentTo(polyA));

	}

	@Test
	public void testIsEquivalentTo_yes() {

		assertTrue(
				new PolygonXZ(Arrays.asList(outlineA0, outlineA1, outlineA4, outlineA0))
					.isEquivalentTo(
						new PolygonXZ(Arrays.asList(outlineA1, outlineA4, outlineA0, outlineA1))));

	}

	@Test
	public void testIsEquivalentTo_no() {

		assertFalse(
				new PolygonXZ(Arrays.asList(outlineA0, outlineA1, outlineA4, outlineA0))
					.isEquivalentTo(
						new PolygonXZ(Arrays.asList(outlineA0, outlineA4, outlineA1, outlineA0))));

	}

	@Test
	public void testIsSelfIntersecting1() {
		VectorXZ v1 = new VectorXZ(0, 0);
		VectorXZ v2 = new VectorXZ(1, 0);
		VectorXZ v3 = new VectorXZ(1, 1);
		VectorXZ v4 = new VectorXZ(0, 1);
		VectorXZ v5 = new VectorXZ(0, 0);

		// Simple rectangle should not be self intersecting
		assertFalse(
				new PolygonXZ(Arrays.asList(v1, v2, v3, v4, v5)).isSelfIntersecting());
	}

	@Test
	public void testIsSelfIntersecting2() {
		VectorXZ v1 = new VectorXZ(1, 5);
		VectorXZ v2 = new VectorXZ(5, 2);
		VectorXZ v3 = new VectorXZ(4, 1);
		VectorXZ v4a = new VectorXZ(3, 4);
		VectorXZ v4b = new VectorXZ(3, 5);
		VectorXZ v4c = new VectorXZ(3, 6);
		VectorXZ v5 = new VectorXZ(1, 5);

		/*
		 * testing a simple self intersecting polygon with 4 vertices
		 * while the line causing the intersection is above, below or at the
		 * same height as the beginning node
		 */

		assertTrue(
				new PolygonXZ(Arrays.asList(v1, v2, v3, v4a, v5)).isSelfIntersecting());

		assertTrue(
				new PolygonXZ(Arrays.asList(v1, v2, v3, v4b, v5)).isSelfIntersecting());

		assertTrue(
				new PolygonXZ(Arrays.asList(v1, v2, v3, v4c, v5)).isSelfIntersecting());
	}

	@Test
	public void testIsSelfIntersecting3() {
		VectorXZ v1 = new VectorXZ(0, 0);

		VectorXZ v2 = new VectorXZ(10, 0);
		VectorXZ v3 = new VectorXZ(15, 0);
		VectorXZ v4 = new VectorXZ(20, 0);

		VectorXZ v5 = new VectorXZ(20, 20);
		VectorXZ v6 = new VectorXZ(0, 20);
		VectorXZ v7 = new VectorXZ(0, 0);

		// Testing simple non self intersecting polygons

		// ... with one vertex is used two times
		assertFalse(
				new PolygonXZ(Arrays.asList(v1, v2, v2, v5, v6, v7)).isSelfIntersecting());

		// ... with 3 parallel line segments
		assertFalse(
				new PolygonXZ(Arrays.asList(v1, v2, v3, v4, v5, v6, v7)).isSelfIntersecting());

		// ... with 3 co-linear line segments
		assertFalse(
				new PolygonXZ(Arrays.asList(v1, v2, v4, v3, v4, v5, v6, v7)).isSelfIntersecting());

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
		assertFalse(
				new PolygonXZ(Arrays.asList(v1, v2, v5, v3, v4, v6, v1)).isSelfIntersecting());

		// ... with one point being visited twice
		assertFalse(
				new PolygonXZ(Arrays.asList(v1, v2, v6, v3, v4, v6, v1)).isSelfIntersecting());

		// .. with a intersection at the right side
		assertTrue(
				new PolygonXZ(Arrays.asList(v1, v2, v6, v3, v4, v5, v1)).isSelfIntersecting());
	}

}
