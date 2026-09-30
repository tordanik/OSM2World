package org.osm2world.scene.mesh;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osm2world.scene.color.Color.RED;
import static org.osm2world.scene.color.Color.YELLOW;
import static org.osm2world.scene.mesh.MeshTestUtil.containsTriangle;
import static org.osm2world.test.TestUtil.assertSameCyclicOrder;

import java.util.*;

import org.junit.Test;
import org.osm2world.math.Angle;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.shapes.LineSegmentXYZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.scene.material.Material.Interpolation;

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

	@Test
	public void testEdges_singleTriangle() {

		var a = new VectorXYZ(0, 0, 0);
		var b = new VectorXYZ(1, 0, 0);
		var c = new VectorXYZ(0, 1, 0);

		TriangleGeometry geometry = geometryOf(new TriangleXYZ(a, b, c));

		Set<Set<VectorXYZ>> expected = Set.of(Set.of(a, b), Set.of(b, c), Set.of(c, a));
		assertEquals(expected, undirected(geometry.edges()));
		assertEquals(expected, undirected(geometry.outerEdges()));

	}

	@Test
	public void testEdges_flatQuad() {

		var a = new VectorXYZ(0, 0, 0);
		var b = new VectorXYZ(1, 0, 0);
		var c = new VectorXYZ(1, 0, 1);
		var d = new VectorXYZ(0, 0, 1);

		TriangleGeometry geometry = geometryOf(new TriangleXYZ(a, b, c), new TriangleXYZ(a, c, d));

		Set<Set<VectorXYZ>> expected = Set.of(Set.of(a, b), Set.of(b, c), Set.of(c, d), Set.of(d, a));
		assertEquals(expected, undirected(geometry.edges()));
		assertEquals(expected, undirected(geometry.outerEdges()));

	}

	@Test
	public void testEdges_foldedQuad() {

		var a = new VectorXYZ(0, 0, 0);
		var b = new VectorXYZ(1, 0, 0);
		var c = new VectorXYZ(1, 1, 1);
		var d = new VectorXYZ(0, 0, 1);

		TriangleGeometry geometry = geometryOf(new TriangleXYZ(a, b, c), new TriangleXYZ(a, c, d));

		assertEquals(Set.of(Set.of(a, b), Set.of(b, c), Set.of(c, d), Set.of(d, a)),
				undirected(geometry.outerEdges()));
		assertEquals(Set.of(Set.of(a, b), Set.of(b, c), Set.of(c, d), Set.of(d, a), Set.of(a, c)),
				undirected(geometry.edges()));

	}

	@Test
	public void testEdges_closedTetrahedron() {

		var a = new VectorXYZ(0, 0, 0);
		var b = new VectorXYZ(1, 0, 0);
		var c = new VectorXYZ(0, 0, 1);
		var d = new VectorXYZ(0, 1, 0);

		TriangleGeometry geometry = geometryOf(
				new TriangleXYZ(a, b, c),
				new TriangleXYZ(a, d, b),
				new TriangleXYZ(b, d, c),
				new TriangleXYZ(c, d, a));

		assertTrue(geometry.outerEdges().isEmpty());
		assertEquals(Set.of(Set.of(a, b), Set.of(a, c), Set.of(a, d), Set.of(b, c), Set.of(b, d), Set.of(c, d)),
				undirected(geometry.edges()));

	}

	private static TriangleGeometry geometryOf(TriangleXYZ... triangles) {
		var builder = new TriangleGeometry.Builder(0, null, Interpolation.FLAT);
		builder.addTriangles(triangles);
		return builder.build();
	}

	/** converts segments to sets of their endpoints to allow comparisons independent of segment direction */
	private static Set<Set<VectorXYZ>> undirected(Collection<LineSegmentXYZ> segments) {
		return segments.stream().map(s -> Set.of(s.p1, s.p2)).collect(toSet());
	}

}
