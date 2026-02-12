package org.osm2world.scene.mesh;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static org.junit.Assert.*;
import static org.osm2world.math.VectorXYZ.*;
import static org.osm2world.math.algorithms.GeometryUtil.closeLoop;
import static org.osm2world.scene.color.Color.WHITE;
import static org.osm2world.scene.mesh.MeshTestUtil.assertContainsQuad;
import static org.osm2world.scene.mesh.MeshTestUtil.containsTriangle;

import org.junit.Test;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.SimpleClosedShapeXZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.math.shapes.TriangleXZ;

public class ShapeGeometryTest {

	@Test
	public void testSimplePolygon() {

		SimpleClosedShapeXZ shape = new SimplePolygonXZ(closeLoop(
				new VectorXZ(-1, -1),
				new VectorXZ( 1, -1),
				new VectorXZ( 2,  1),
				new VectorXZ( 1,  1)));

		ShapeGeometry shapeGeometry = new ShapeGeometry(shape, new VectorXYZ(0, 5, 0), X_UNIT, Y_UNIT, 1.0, WHITE, emptyList());
		TriangleGeometry triangleGeometry = shapeGeometry.asTriangles();

		assertEquals(2, triangleGeometry.triangles.size());
		assertContainsQuad(triangleGeometry.triangles,
				new VectorXYZ(0, 4, -1), new VectorXYZ(0, 4, 1), new VectorXYZ(0, 6, 2), new VectorXYZ(0, 6, 1));

		assertEquals(nCopies(6, WHITE), triangleGeometry.colors);
		assertEquals(nCopies(6, X_UNIT), triangleGeometry.normalData.normals());

	}

	@Test
	public void testScaledTriangle() {

		TriangleXZ shape = new TriangleXZ(
				new VectorXZ(0, 0),
				new VectorXZ(1, 0),
				new VectorXZ(0, 1));

		ShapeGeometry shapeGeometry = new ShapeGeometry(shape, new VectorXYZ(0, 0, 10),
				Y_UNIT.invert(), Z_UNIT.invert(), 10.0, null, emptyList());
		TriangleGeometry triangleGeometry = shapeGeometry.asTriangles();

		assertEquals(1, triangleGeometry.triangles.size());
		assertTrue(containsTriangle(triangleGeometry.triangles,
				new VectorXYZ(0, 0, 10),
				new VectorXYZ(10, 0, 10),
				new VectorXYZ(0, 0, 0)));

		assertNull(triangleGeometry.colors);
		assertEquals(nCopies(3, Y_UNIT.invert()), triangleGeometry.normalData.normals());

	}

}
