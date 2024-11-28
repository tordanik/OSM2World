package org.osm2world.core.math.algorithms;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.osm2world.core.math.GeometryUtil.closeLoop;
import static org.osm2world.core.math.SimplePolygonXZ.asSimplePolygon;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.math.algorithms.FaceDecompositionUtil.facesFromGraph;
import static org.osm2world.core.math.algorithms.FaceDecompositionUtil.splitPolygonIntoFaces;
import static org.osm2world.core.test.TestUtil.assertSameCyclicOrder;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.osm2world.core.math.*;
import org.osm2world.core.math.shapes.CircleXZ;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.math.shapes.SimplePolygonShapeXZ;

public class FaceDecompositionUtilTest {

	private final VectorXZ topOuter = new VectorXZ(0, +8);
	private final VectorXZ topInner = new VectorXZ(0, +4);
	private final VectorXZ leftOuter = new VectorXZ(-8, 0);
	private final VectorXZ leftInner = new VectorXZ(-4, 0);
	private final VectorXZ bottomOuter = new VectorXZ(0, -8);
	private final VectorXZ bottomInner = new VectorXZ(0, -4);
	private final VectorXZ rightOuter = new VectorXZ(+8, 0);
	private final VectorXZ rightInner = new VectorXZ(+4, 0);

	/** a "donut" shape consisting of an outer and inner square ring, standing on edge. Used in multiple tests. */
	private final PolygonShapeXZ diamondDonut = new PolygonWithHolesXZ(
			new SimplePolygonXZ(closeLoop(topOuter, leftOuter, bottomOuter, rightOuter)),
			List.of(new SimplePolygonXZ(closeLoop(topInner, leftInner, bottomInner, rightInner))));

	@Test
	public void testFacesFromGraph() {

		List<VectorXZ> vs = asList(
				new VectorXZ(0, -5),
				new VectorXZ(5, 5),
				new VectorXZ(-5, 5));

		List<LineSegmentXZ> segments = asList(
				new LineSegmentXZ(vs.get(0), vs.get(1)),
				new LineSegmentXZ(vs.get(1), vs.get(2)),
				new LineSegmentXZ(vs.get(2), vs.get(1)), // intentional duplicate
				new LineSegmentXZ(vs.get(0), vs.get(2)));

		Collection<PolygonWithHolesXZ> result = facesFromGraph(segments);

		assertEquals(1, result.size());
		PolygonShapeXZ resultPoly = result.iterator().next();
		assertSameCyclicOrder(true, vs, resultPoly.verticesNoDup());

	}

	@Test
	public void testWithoutOtherShapes() {

		List<PolygonShapeXZ> testInput = asList(
				asSimplePolygon(new AxisAlignedRectangleXZ(NULL_VECTOR, 10, 50)),
				asSimplePolygon(new AxisAlignedRectangleXZ(new VectorXZ(-30, -40), 10, 50)),
				asSimplePolygon(new CircleXZ(new VectorXZ(5, 10), 33)),
				diamondDonut);

		for (PolygonShapeXZ polygon : testInput) {

			Collection<PolygonWithHolesXZ> result = splitPolygonIntoFaces(polygon, emptyList(), emptyList());

			assertEquals("shape: " + polygon, 1, result.size());
			PolygonShapeXZ resultPoly = result.iterator().next();
			assertSameCyclicOrder(true, polygon.verticesNoDup(), resultPoly.verticesNoDup());
			assertEquals(polygon.getHoles().size(), resultPoly.getHoles().size());

		}

	}

	@Test
	public void testQuartering() {

		List<PolygonShapeXZ> testInput = asList(
				asSimplePolygon(new AxisAlignedRectangleXZ(NULL_VECTOR, 10, 50)),
				asSimplePolygon(new AxisAlignedRectangleXZ(new VectorXZ(-30, -40), 10, 50)),
				asSimplePolygon(new CircleXZ(new VectorXZ(5, 10), 33)),
				diamondDonut);

		for (PolygonShapeXZ polygon : testInput) {

			VectorXZ center = polygon.getOuter().getCentroid();
			LineSegmentXZ horizontalLine = new LineSegmentXZ(center.add(-200, 0), center.add(+200, 0));
			LineSegmentXZ verticalLine = new LineSegmentXZ(center.add(0, -200), center.add(0, +200));

			Collection<PolygonWithHolesXZ> result = splitPolygonIntoFaces(polygon, List.of(), List.of(horizontalLine, verticalLine));

			assertEquals(4, result.size());

		}

	}

	@Test
	public void testShapeWithHole() {

		List<LineSegmentXZ> otherShapes = asList(
				new LineSegmentXZ(topOuter, topInner),
				new LineSegmentXZ(topInner, topOuter), // intentional reversed duplicate
				new LineSegmentXZ(bottomInner, bottomOuter),
				new LineSegmentXZ(leftOuter, leftInner),
				new LineSegmentXZ(bottomInner, leftInner), // duplicate of an existing edge
				new LineSegmentXZ(rightOuter, leftInner)); // intentionally "too long"

		Collection<PolygonWithHolesXZ> result = splitPolygonIntoFaces(diamondDonut, List.of(), otherShapes);

		assertEquals(4, result.size());

		for (PolygonShapeXZ resultPoly : result) {
			SimplePolygonShapeXZ outer = resultPoly.getOuter();
			if (outer.getCentroid().x > 0 && outer.getCentroid().z > 0) {
				assertSameCyclicOrder(true, asList(topInner, topOuter, rightOuter, rightInner), outer.verticesNoDup());
			} else if (outer.getCentroid().x < 0 && outer.getCentroid().z > 0) {
				assertSameCyclicOrder(true, asList(topInner, topOuter, leftOuter, leftInner), outer.verticesNoDup());
			} else if (outer.getCentroid().x > 0 && outer.getCentroid().z < 0) {
				assertSameCyclicOrder(true, asList(bottomInner, bottomOuter, rightOuter, rightInner), outer.verticesNoDup());
			} else if (outer.getCentroid().x < 0 && outer.getCentroid().z < 0) {
				assertSameCyclicOrder(true, asList(bottomInner, bottomOuter, leftOuter, leftInner), outer.verticesNoDup());
			} else {
				fail("wrong result poly: " + outer);
			}
		}

	}

	@Test
	public void testAccuracy() { // with this data, the area for the otherShape is very slightly off when it's reversed

		PolygonShapeXZ polygon = new SimplePolygonXZ(closeLoop(
				new VectorXZ(0.0, 0.0),
				new VectorXZ(2.0, 0.0),
				new VectorXZ(2.0, 3.0),
				new VectorXZ(0.0, 3.0)));

		Collection<? extends ShapeXZ> otherShapes = singletonList(new SimplePolygonXZ(closeLoop(
				new VectorXZ(0.1, 0.75),
				new VectorXZ(1.167323103354597, 0.75),
				new VectorXZ(1.167323103354597, 2.0000000298023224),
				new VectorXZ(0.1, 2.0000000298023224))));

		Collection<PolygonWithHolesXZ> result = splitPolygonIntoFaces(polygon, List.of(), otherShapes);

		assertEquals(2, result.size());

	}

}
