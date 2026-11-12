package org.osm2world.math.algorithms;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.math.shapes.TriangleXZ;

public class TinfourTriangulationUtilTest extends AbstractTriangulationUtilTest {

	@Override
	protected Collection<TriangleXZ> triangulate(PolygonShapeXZ p, Collection<VectorXZ> points) {
		return TinfourTriangulationUtil.triangulateWithoutAddingVertices(p.getOuter(), p.getHoles(), points);
	}

	@Test
	public void testPointsAndHoles() {

		SimplePolygonXZ square = new AxisAlignedRectangleXZ(0, 0, 10, 10).polygonXZ();
		SimplePolygonXZ hole = new AxisAlignedRectangleXZ(6, 6, 8, 8).polygonXZ();

		var inside1 = new VectorXZ(2, 3);
		var inside2 = new VectorXZ(4, 7);
		var onBoundary = new VectorXZ(0, 5);
		var nearBoundary = new VectorXZ(5, 9.9995);
		var inHole = new VectorXZ(7, 7);
		var outside = new VectorXZ(20, 20);

		List<TriangleXZ> triangles = TinfourTriangulationUtil.triangulateWithoutAddingVertices(square, List.of(hole),
				List.of(inside1, inside2, onBoundary, nearBoundary, inHole, outside));

		assertEquals(96, triangles.stream().mapToDouble(TriangleXZ::getArea).sum(), 1e-6);

		Set<VectorXZ> expectedVertices = new HashSet<>(square.verticesNoDup());
		expectedVertices.addAll(hole.verticesNoDup());
		expectedVertices.add(inside1);
		expectedVertices.add(inside2);
		assertEquals(expectedVertices, vertices(triangles));

	}

	private static Set<VectorXZ> vertices(List<TriangleXZ> triangles) {
		Set<VectorXZ> result = new HashSet<>();
		triangles.forEach(t -> result.addAll(t.verticesNoDup()));
		return result;
	}

}
