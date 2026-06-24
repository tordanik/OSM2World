package org.osm2world.math.algorithms;

import static org.junit.Assert.assertEquals;
import static org.osm2world.math.algorithms.GeometryUtil.closeLoop;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.math.shapes.TriangleXZ;

public class Earcut4JTriangulationUtilTest extends AbstractTriangulationUtilTest {

	@Override
	protected Collection<TriangleXZ> triangulate(PolygonShapeXZ p, Collection<VectorXZ> points) {
		return Earcut4JTriangulationUtil.triangulate(p.getOuter(), p.getHoles(), points);
	}

	@Test
	public void testTriangulate_rectangleWithHole() {

		var outer = new SimplePolygonXZ(closeLoop(
				new VectorXZ(0, 0),
				new VectorXZ(1, 0),
				new VectorXZ(1, 1),
				new VectorXZ(0, 1)
		));

		SimplePolygonXZ inner = new SimplePolygonXZ(closeLoop(
				new VectorXZ(0.25, 0.25),
				new VectorXZ(0.75, 0.25),
				new VectorXZ(0.75, 0.75),
				new VectorXZ(0.25, 0.75)
		));

		Collection<TriangleXZ> result = triangulate(new PolygonWithHolesXZ(outer, List.of(inner)), List.of());

		assertEquals(8, result.size());
		assertAlmostEquals(outer.getArea() - inner.getArea(), result.stream().mapToDouble(TriangleXZ::getArea).sum());

	}

}
