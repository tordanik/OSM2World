package org.osm2world.scene.mesh;

import static com.google.common.collect.Lists.reverse;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osm2world.math.VectorXYZ.Y_UNIT;
import static org.osm2world.math.VectorXYZ.Z_UNIT;
import static org.osm2world.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.math.algorithms.GeometryUtil.closeLoop;
import static org.osm2world.scene.mesh.MeshTestUtil.assertContainsQuad;
import static org.osm2world.scene.mesh.MeshTestUtil.containsTriangle;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.junit.Test;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.*;
import org.osm2world.output.common.ExtrudeOption;

public class ExtrusionGeometryTest {

	@Test
	public void testExtrudeLineSegment() {

		ShapeXZ shape = new LineSegmentXZ(new VectorXZ(2, 0), new VectorXZ(-1, 0));

		List<VectorXYZ> path = asList(
				new VectorXYZ(0, 0, 0),
				new VectorXYZ(0, 1, 0),
				new VectorXYZ(0, 1, 1));

		ExtrusionGeometry eg = new ExtrusionGeometry(shape, path, asList(Z_UNIT.invert(), Y_UNIT, Y_UNIT),
				null, null, null, emptyList());
		List<TriangleXYZ> result = eg.asTriangles().triangles;

		assertEquals(4, result.size());

		assertContainsQuad(result, new VectorXYZ(-2, 0, 0), new VectorXYZ(1, 0, 0),
				new VectorXYZ(1, 1, 0), new VectorXYZ(-2, 1, 0));
		assertContainsQuad(result, new VectorXYZ(-2, 1, 0), new VectorXYZ(1, 1, 0),
				new VectorXYZ(1, 1, 1), new VectorXYZ(-2, 1, 1));

	}

	@Test
	public void testExtrudeTriangleIntoPyramid() {

		ShapeXZ shape = new TriangleXZ(new VectorXZ(-1, 0), new VectorXZ(1, 0), new VectorXZ(0, 2));

		List<VectorXYZ> path = asList(
				new VectorXYZ(0, 0, 0),
				new VectorXYZ(0, 1, 0));

		List<List<TriangleXYZ>> results = new ArrayList<>();

		{ /* path from bottom to top */

			ExtrusionGeometry eg = new ExtrusionGeometry(shape, path, nCopies(2, Z_UNIT),
					asList(1.0, 0.0), null, EnumSet.of(ExtrudeOption.START_CAP), emptyList());

			results.add(eg.asTriangles().triangles);

		} { /* path from top to bottom (should produce same result for symmetric shape) */

			ExtrusionGeometry eg = new ExtrusionGeometry(shape, reverse(path), nCopies(2, Z_UNIT),
					asList(0.0, 1.0), null, EnumSet.of(ExtrudeOption.END_CAP), emptyList());

			results.add(eg.asTriangles().triangles);

		}

		/* check results */

		for (List<TriangleXYZ> result : results) {

			assertEquals(4, result.size());

			assertTrue(containsTriangle(result,
					new VectorXYZ(-1, 0, 0),
					new VectorXYZ(1, 0, 0),
					new VectorXYZ(0, 1, 0)));
			assertTrue(containsTriangle(result,
					new VectorXYZ(1, 0, 0),
					new VectorXYZ(0, 0, 2),
					new VectorXYZ(0, 1, 0)));
			assertTrue(containsTriangle(result,
					new VectorXYZ(0, 0, 2),
					new VectorXYZ(-1, 0, 0),
					new VectorXYZ(0, 1, 0)));

			assertTrue(containsTriangle(result,
					new VectorXYZ(1, 0, 0),
					new VectorXYZ(-1, 0, 0),
					new VectorXYZ(0, 0, 2)));

		}

	}

	/** tests extruding a rectangle into a (slanted) wall with caps at both ends */
	@Test
	public void testExtrudeRectangleWithTwoCaps() {

		ShapeXZ shape = new SimplePolygonXZ(closeLoop(
				NULL_VECTOR,
				new VectorXZ(1, 0),
				new VectorXZ(1, 2),
				new VectorXZ(0, 1.5)));

		List<VectorXYZ> path = asList(
				new VectorXYZ(5, 0, 0),
				new VectorXYZ(5, 0, -1));

		ExtrusionGeometry eg = new ExtrusionGeometry(shape, path, nCopies(2, Y_UNIT),
				null, null, EnumSet.of(ExtrudeOption.START_CAP, ExtrudeOption.END_CAP), emptyList());

		List<TriangleXYZ> result = eg.asTriangles().triangles;

		/* check results */

		assertEquals(12, result.size());

		//top
		assertContainsQuad(result, new VectorXYZ(5, 1.5, -1),new VectorXYZ(6, 2, -1),
				new VectorXYZ(6, 2, 0), new VectorXYZ(5, 1.5, 0));

		//bottom
		assertContainsQuad(result, new VectorXYZ(6, 0, -1),new VectorXYZ(5, 0, -1),
				new VectorXYZ(5, 0, 0), new VectorXYZ(6, 0, 0));

		//sides
		assertContainsQuad(result, new VectorXYZ(5, 0, 0),new VectorXYZ(5, 0, -1),
				new VectorXYZ(5, 1.5, -1), new VectorXYZ(5, 1.5, 0));
		assertContainsQuad(result, new VectorXYZ(6, 0, -1),new VectorXYZ(6, 0, 0),
				new VectorXYZ(6, 2, 0), new VectorXYZ(6, 2, -1));

		//end and start cap
		assertContainsQuad(result, new VectorXYZ(5, 0, -1),new VectorXYZ(6, 0, -1),
				new VectorXYZ(6, 2, -1), new VectorXYZ(5, 1.5, -1));
		assertContainsQuad(result, new VectorXYZ(6, 0, 0),new VectorXYZ(5, 0, 0),
				new VectorXYZ(5, 1.5, 0), new VectorXYZ(6, 2, 0));

	}

	@Test
	public void testExtrusionNormals() {



	}

}
