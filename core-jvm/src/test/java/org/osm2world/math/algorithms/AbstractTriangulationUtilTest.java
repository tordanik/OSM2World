package org.osm2world.math.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osm2world.math.algorithms.GeometryUtil.closeLoop;
import static org.osm2world.test.TestUtil.assertAlmostEquals;
import static org.osm2world.test.TestUtil.assertSameCyclicOrder;

import java.util.Collection;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.math.shapes.TriangleXZ;


public abstract class AbstractTriangulationUtilTest {

	protected abstract Collection<TriangleXZ> triangulate(PolygonShapeXZ p, Collection<VectorXZ> points);

	@Test
	public void testTriangulate() {

		var polygonA = new SimplePolygonXZ(closeLoop(
				new VectorXZ(-1.1f, -1.1f),
				new VectorXZ(-1.1f, 1.1f),
				new VectorXZ(1.1f, 1.1f),
				new VectorXZ(1.1f, -1.1f)));

		var polygonB = new SimplePolygonXZ(closeLoop(
				new VectorXZ(100, 0),
				new VectorXZ(0, 100),
				new VectorXZ(-99, 0),
				new VectorXZ(0, -99)));

		for (SimplePolygonXZ polygon : List.of(polygonA, polygonB)) {
			Collection<TriangleXZ> triangles = triangulate(polygon, List.of());
			assertEquals(2, triangles.size());
			assertAlmostEquals(polygon.getArea(), triangles.stream().mapToDouble(TriangleXZ::getArea).sum());
		}

		PolygonWithHolesXZ polygonWithHoles = new PolygonWithHolesXZ(polygonB, List.of(polygonA));
		Collection<TriangleXZ> triangles = triangulate(polygonWithHoles, List.of());
		assertAlmostEquals(polygonB.getArea() - polygonA.getArea(),
				triangles.stream().mapToDouble(TriangleXZ::getArea).sum());

	}

	@Test
	public void testTriangulate_triangle() {

		var outer = new SimplePolygonXZ(closeLoop(
				new VectorXZ(-1, 0),
				new VectorXZ(1, 0),
				new VectorXZ(0, 1)
		));

		Collection<TriangleXZ> result = triangulate(outer, List.of());

		assertEquals(1, result.size());
		assertSameCyclicOrder(true, result.iterator().next().getVertices(), outer.getVertex(0), outer.getVertex(1), outer.getVertex(2));

	}

	@Test
	public void testTriangulate_rectangle() {

		var outer = new SimplePolygonXZ(closeLoop(
				new VectorXZ(0, 0),
				new VectorXZ(1, 0),
				new VectorXZ(1, 1),
				new VectorXZ(0, 1)
		));

		Collection<TriangleXZ> result = triangulate(outer, List.of());

		assertEquals(2, result.size());

	}

	@Ignore // points are currently ignored in Earcut4JTriangulationUtil
	@Test
	public void testTriangulate_triangleWithPoint() {

		var outer = new SimplePolygonXZ(closeLoop(
				new VectorXZ(-1, 0),
				new VectorXZ(1, 0),
				new VectorXZ(0, 1)
		));

		VectorXZ point = new VectorXZ(0, 0.3);

		Collection<TriangleXZ> result = triangulate(outer, List.of(point));

		assertTrue(result.size() >= 3);
		assertAlmostEquals(outer.getArea(), result.stream().mapToDouble(TriangleXZ::getArea).sum());

	}

	@Ignore // TODO: fix the triangulation errors that happen with a sufficient number of inner points
	@Test
	public void testTriangulate_multiplePoints() {

		var outer = new SimplePolygonXZ(closeLoop(
				new VectorXZ(-10, -10),
				new VectorXZ(+10, -10),
				new VectorXZ(+10, +10),
				new VectorXZ(-10, +10)
		));

		List<VectorXZ> points = List.of(
				new VectorXZ(-5, -5),
				new VectorXZ(-3, 4),
				new VectorXZ(3, 3),
				new VectorXZ(5, -2));

		Collection<TriangleXZ> result = triangulate(outer, points);

		assertTrue(result.size() > 5);

		for (VectorXZ point : points) {
			assertTrue(result.stream().anyMatch(t -> t.vertices().contains(point)));
		}

	}

}
