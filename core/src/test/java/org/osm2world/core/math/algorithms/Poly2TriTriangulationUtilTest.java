package org.osm2world.core.math.algorithms;

import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.LineSegmentXZ;
import org.osm2world.core.math.shapes.SimplePolygonXZ;
import org.osm2world.core.math.shapes.TriangleXZ;
import org.osm2world.core.util.exception.TriangulationException;


public class Poly2TriTriangulationUtilTest {

	private static final VectorXZ outlineA0 = new VectorXZ(-1.1f, -1.1f);
	private static final VectorXZ outlineA1 = new VectorXZ(-1.1f, 1.1f);
	private static final VectorXZ outlineA2 = new VectorXZ(1.1f, 1.1f);
	private static final VectorXZ outlineA3 = new VectorXZ(1.1f, -1.1f);

	private static final List<VectorXZ> outlineA = Arrays.asList(outlineA0,
			outlineA1, outlineA2, outlineA3, outlineA0);

	private static final List<VectorXZ> outlineB = Arrays.asList(
			new VectorXZ(100, 0),
			new VectorXZ(0, 100),
			new VectorXZ(-99, 0),
			new VectorXZ(0, -99),
			new VectorXZ(100, 0));

	@Test
	public void triangulateTest() throws TriangulationException {

		SimplePolygonXZ polygon = new SimplePolygonXZ(outlineB);

		List<TriangleXZ> triangles = Poly2TriTriangulationUtil.triangulate(
				polygon,
				Collections.<SimplePolygonXZ>emptyList(),
				Collections.<LineSegmentXZ>emptyList(),
				Collections.<VectorXZ>emptyList());

		double triangleArea = 0;

		for (TriangleXZ t : triangles) {
			triangleArea += t.getArea();
		}

		assertAlmostEquals(polygon.getArea(), triangleArea);

	}

}
