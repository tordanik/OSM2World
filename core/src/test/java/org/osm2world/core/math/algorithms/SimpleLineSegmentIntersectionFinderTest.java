package org.osm2world.core.math.algorithms;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.osm2world.core.math.algorithms.SimpleLineSegmentIntersectionFinder.findAllIntersections;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.LineSegmentXZ;

//TODO deduplicate with LineSegmentIntersectionFinderTest (or remove this test along with the SimpleLSIF implementation)
public class SimpleLineSegmentIntersectionFinderTest {

	@Test
	public void testFindAllIntersections1() {

		List<LineSegmentXZ> testData = asList(
				new LineSegmentXZ(new VectorXZ(0, 0), new VectorXZ(10, 0)),
				new LineSegmentXZ(new VectorXZ(0, -5), new VectorXZ(10, +5))
		);

		Set<VectorXZ> result = new HashSet<>();
		findAllIntersections(testData).forEach(it -> result.add(it.pos()));

		assertAlmostEquals(newHashSet(new VectorXZ(5, 0)), result);

	}

	@Test
	public void testFindAllIntersections2() {

		// similar to the first test, but with short segments "hiding" the two intersecting ones from each other for a bit

		List<LineSegmentXZ> testData = asList(
				new LineSegmentXZ(new VectorXZ(-10, +5), new VectorXZ(10, -5)),
				new LineSegmentXZ(new VectorXZ(-9, -5), new VectorXZ(+9, +5)),
				new LineSegmentXZ(new VectorXZ(-20, 0), new VectorXZ(-3, 0)),
				new LineSegmentXZ(new VectorXZ(-2, 0), new VectorXZ(-1, 0)),
				new LineSegmentXZ(new VectorXZ(+3, 0), new VectorXZ(+6, 0))
		);

		Set<VectorXZ> result = new HashSet<>();
		findAllIntersections(testData).forEach(it -> result.add(it.pos()));

		assertAlmostEquals(newHashSet(new VectorXZ(0, 0)), result);

	}

	@Test
	public void testFindAllIntersections3() {

		List<LineSegmentXZ> testData = asList(
				new LineSegmentXZ(new VectorXZ(-10, -10), new VectorXZ(+10, +10)),
				new LineSegmentXZ(new VectorXZ(-8, -9), new VectorXZ(+9, +8)),
				new LineSegmentXZ(new VectorXZ(-5, +5), new VectorXZ(+5, -5))
		);

		Set<VectorXZ> result = new HashSet<>();
		findAllIntersections(testData).forEach(it -> result.add(it.pos()));

		assertAlmostEquals(newHashSet(new VectorXZ(0, 0), new VectorXZ(0.5, -0.5)), result);

	}

	@Test
	public void testFindAllIntersections_vertical() {

		// has vertical segments

		List<LineSegmentXZ> testData = asList(
				new LineSegmentXZ(new VectorXZ(2, -10), new VectorXZ(2, +5)),
				new LineSegmentXZ(new VectorXZ(4, -5), new VectorXZ(4, +10)),
				new LineSegmentXZ(new VectorXZ(6, -10), new VectorXZ(6, +5)),
				new LineSegmentXZ(new VectorXZ(3, 3), new VectorXZ(5, 5))
		);

		Set<VectorXZ> result = new HashSet<>();
		findAllIntersections(testData).forEach(it -> result.add(it.pos()));

		assertAlmostEquals(newHashSet(new VectorXZ(4, 4)), result);

	}

	@Test
	public void testFindAllIntersections_sharedNode() {

		List<LineSegmentXZ> testData = asList(
				new LineSegmentXZ(new VectorXZ(0, 5), new VectorXZ(0, 0)),
				new LineSegmentXZ(new VectorXZ(5, 0), new VectorXZ(0, 0))
		);

		assertEquals(emptyList(), findAllIntersections(testData));

	}

	@Test
	public void testFindAllIntersections_3horiz1vert() {

		List<LineSegmentXZ> testData = new ArrayList<>();

		testData.add(new LineSegmentXZ(new VectorXZ(-5, -5), new VectorXZ(5, -5)));
		testData.add(new LineSegmentXZ(new VectorXZ(-5, 5), new VectorXZ(5, 5)));
		testData.add(new LineSegmentXZ(new VectorXZ(-10, 0), new VectorXZ(+10, 0)));
		testData.add(new LineSegmentXZ(new VectorXZ(0, -20), new VectorXZ(0, +20)));

		assertEquals(3, findAllIntersections(testData).size());

	}

	@Ignore //known bug
	@Test
	public void testFindAllIntersections_crosshairRectangle() {

		List<LineSegmentXZ> testData = new ArrayList<>();

		testData.add(new LineSegmentXZ(new VectorXZ(-5, -5), new VectorXZ(5, -5)));
		testData.add(new LineSegmentXZ(new VectorXZ(5, -5), new VectorXZ(5, 5)));
		testData.add(new LineSegmentXZ(new VectorXZ(5, 5), new VectorXZ(-5, 5)));
		testData.add(new LineSegmentXZ(new VectorXZ(-5, 5), new VectorXZ(-5, -5)));
		testData.add(new LineSegmentXZ(new VectorXZ(-10, 0), new VectorXZ(+10, 0)));
		testData.add(new LineSegmentXZ(new VectorXZ(0, -20), new VectorXZ(0, +20)));

		assertEquals(5, findAllIntersections(testData).size());

	}

	@Ignore //known limitation of the implementation
	@Test
	public void testFindAllIntersections_tripleIntersection() {

		List<LineSegmentXZ> testData = asList(
				new LineSegmentXZ(new VectorXZ(0, 0), new VectorXZ(10, 0)),
				new LineSegmentXZ(new VectorXZ(0, -5), new VectorXZ(10, +5)),
				new LineSegmentXZ(new VectorXZ(0, +5), new VectorXZ(10, -5))
		);

		assertEquals(3, findAllIntersections(testData).size());

	}

}
