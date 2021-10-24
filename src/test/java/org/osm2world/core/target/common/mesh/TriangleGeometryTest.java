package org.osm2world.core.target.common.mesh;

import static java.awt.Color.RED;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.junit.Assert.*;
import static org.osm2world.core.target.common.mesh.MeshTestUtil.containsTriangle;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.material.Material.Interpolation;

public class TriangleGeometryTest {

	@Test
	public void testSmoothTriangleStrip() {

		TriangleGeometry.Builder builder = new TriangleGeometry.Builder(RED, Interpolation.SMOOTH);
		builder.addTriangleStrip(asList(
				new VectorXYZ(0, 1, 0), new VectorXYZ(0, 0, 0),
				new VectorXYZ(1, 1, 0), new VectorXYZ(1, 0, 0),
				new VectorXYZ(2, 1, 0), new VectorXYZ(2, 0, 0),
				new VectorXYZ(2, 1, -1), new VectorXYZ(2, 0, -1)));
		builder.setTexCoordFunctions(emptyList());
		TriangleGeometry geometry = builder.build();

		assertEquals(6, geometry.triangles.size());
		assertEquals(18, geometry.vertices().size());

		assertTrue(containsTriangle(geometry.triangles,
				new VectorXYZ(0, 1, 0), new VectorXYZ(0, 0, 0), new VectorXYZ(1, 1, 0)));
		assertTrue(containsTriangle(geometry.triangles,
				new VectorXYZ(0, 0, 0), new VectorXYZ(1, 0, 0), new VectorXYZ(1, 1, 0)));

		assertTrue(containsTriangle(geometry.triangles,
				new VectorXYZ(1, 1, 0), new VectorXYZ(1, 0, 0), new VectorXYZ(2, 1, 0)));
		assertTrue(containsTriangle(geometry.triangles,
				new VectorXYZ(1, 0, 0), new VectorXYZ(2, 0, 0), new VectorXYZ(2, 1, 0)));

		Map<VectorXYZ, VectorXYZ> expectedNormals = new HashMap<>();
		expectedNormals.put(new VectorXYZ(0, 0, 0), new VectorXYZ(0, 0, -1));
		expectedNormals.put(new VectorXYZ(0, 1, 0), new VectorXYZ(0, 0, -1));
		expectedNormals.put(new VectorXYZ(1, 0, 0), new VectorXYZ(0, 0, -1));
		expectedNormals.put(new VectorXYZ(1, 1, 0), new VectorXYZ(0, 0, -1));
		expectedNormals.put(new VectorXYZ(2, 0, -1), new VectorXYZ(-1, 0, 0));
		expectedNormals.put(new VectorXYZ(2, 1, -1), new VectorXYZ(-1, 0, 0));

		for (VectorXYZ v : expectedNormals.keySet()) {
			for (int i = 0; i < geometry.vertices().size(); i++) {
				if (geometry.vertices().get(i).equals(v)) {
					assertEquals(expectedNormals.get(v), geometry.normalData.normals().get(i));
				}
			}
		}

		assertEquals(nCopies(18, RED), geometry.colors);

	}

}
