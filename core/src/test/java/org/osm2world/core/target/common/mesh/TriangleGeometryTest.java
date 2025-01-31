package org.osm2world.core.target.common.mesh;

import static java.awt.Color.RED;
import static java.awt.Color.YELLOW;
import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osm2world.core.target.common.mesh.MeshTestUtil.containsTriangle;
import static org.osm2world.core.test.TestUtil.assertSameCyclicOrder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.osm2world.core.math.Angle;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.shapes.TriangleXYZ;
import org.osm2world.core.target.common.material.Material.Interpolation;

public class TriangleGeometryTest {

	@Test
	public void testSmoothTriangleStrip() {

		TriangleGeometry.Builder builder = new TriangleGeometry.Builder(0, RED, Interpolation.SMOOTH);
		builder.addTriangleStrip(asList(
				new VectorXYZ(0, 1, 0), new VectorXYZ(0, 0, 0),
				new VectorXYZ(1, 1, 0), new VectorXYZ(1, 0, 0),
				new VectorXYZ(2, 1, 0), new VectorXYZ(2, 0, 0),
				new VectorXYZ(2, 1, -1), new VectorXYZ(2, 0, -1)),
				emptyList());
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

	@Test
	public void testTransform() {

		var t = new TriangleXYZ(new VectorXYZ(0, 0, 0), new VectorXYZ(1, 0, 0), new VectorXYZ(0, 1, 0));

		var builder = new TriangleGeometry.Builder(0, YELLOW, Interpolation.FLAT);
		builder.addTriangles(t);
		TriangleGeometry geometry = builder.build();

		List<TriangleXYZ> result0 = geometry.transform(new VectorXYZ(0, 5, 0), null, null).triangles;
		assertEquals(1, result0.size());
		assertSameCyclicOrder(false, result0.get(0).verticesNoDup(),
				new VectorXYZ(0, 5, 0), new VectorXYZ(1, 5, 0), new VectorXYZ(0, 6, 0));

		List<TriangleXYZ> result1 = geometry.transform(new VectorXYZ(0, 5, 0), Angle.ofRadians(-PI/2), null).triangles;
		assertEquals(1, result1.size());
		assertSameCyclicOrder(false, result1.get(0).verticesNoDup(),
				new VectorXYZ(0, 5, 0), new VectorXYZ(0, 5, 1), new VectorXYZ(0, 6, 0));

		List<TriangleXYZ> result2 = geometry.transform(new VectorXYZ(0, 0, 0), Angle.ofRadians(-PI/2), 0.5).triangles;
		assertEquals(1, result2.size());
		assertSameCyclicOrder(false, result2.get(0).verticesNoDup(),
				new VectorXYZ(0, 0, 0), new VectorXYZ(0, 0, 0.5), new VectorXYZ(0, 0.5, 0));

	}

}
