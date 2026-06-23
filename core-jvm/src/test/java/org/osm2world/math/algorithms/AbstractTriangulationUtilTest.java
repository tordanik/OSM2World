package org.osm2world.math.algorithms;

import static org.junit.Assert.assertEquals;
import static org.osm2world.math.algorithms.GeometryUtil.closeLoop;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.math.shapes.TriangleXZ;


public abstract class AbstractTriangulationUtilTest {

	protected static final SimplePolygonXZ polygonA = new SimplePolygonXZ(closeLoop(
			new VectorXZ(-1.1f, -1.1f),
			new VectorXZ(-1.1f, 1.1f),
			new VectorXZ(1.1f, 1.1f),
			new VectorXZ(1.1f, -1.1f)));

	protected static final SimplePolygonXZ polygonB = new SimplePolygonXZ(closeLoop(
			new VectorXZ(100, 0),
			new VectorXZ(0, 100),
			new VectorXZ(-99, 0),
			new VectorXZ(0, -99)));

	public void testTriangulate(Function<PolygonShapeXZ, Collection<TriangleXZ>> triangulate) {

		for (SimplePolygonXZ polygon : List.of(polygonA, polygonB)) {
			Collection<TriangleXZ> triangles = triangulate.apply(polygon);
			assertEquals(2, triangles.size());
			assertAlmostEquals(polygon.getArea(), triangles.stream().mapToDouble(TriangleXZ::getArea).sum());
		}

		PolygonWithHolesXZ polygonWithHoles = new PolygonWithHolesXZ(polygonB, List.of(polygonA));
		Collection<TriangleXZ> triangles = triangulate.apply(polygonWithHoles);
		assertAlmostEquals(polygonB.getArea() - polygonA.getArea(),
				triangles.stream().mapToDouble(TriangleXZ::getArea).sum());

	}

}
