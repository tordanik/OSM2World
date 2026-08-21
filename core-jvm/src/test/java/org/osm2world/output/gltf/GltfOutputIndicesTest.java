package org.osm2world.output.gltf;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osm2world.output.gltf.GltfOutputTestUtil.sideLengths;
import static org.osm2world.output.gltf.GltfOutputTestUtil.writeGltf;
import static org.osm2world.scene.material.Material.Interpolation.FLAT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.mesh.TriangleGeometry;
import org.osm2world.util.platform.image.ImageImplementationJvm;
import org.osm2world.util.platform.json.JsonImplementationJvm;

/**
 * tests that {@link GltfOutput} writes indexed geometry, and that the geometry survives it
 */
public class GltfOutputIndicesTest {

	static {
		JsonImplementationJvm.register();
		ImageImplementationJvm.register();
	}

	@Test
	public void testSharedVerticesAreStoredOnlyOnce() throws IOException {

		/* a quad made of two triangles: the two vertices along the shared edge can be re-used */

		var v00 = new VectorXYZ(0, 0, 0);
		var v10 = new VectorXYZ(1, 0, 0);
		var v01 = new VectorXYZ(0, 0, 1);
		var v11 = new VectorXYZ(1, 0, 1);

		List<TriangleXYZ> quad = List.of(
				new TriangleXYZ(v00, v10, v11),
				new TriangleXYZ(v00, v11, v01));

		GltfModel model = writeAndReadGltf(quad);

		assertEquals(2, triangles(model).size());

		/* 6 vertices without indices, 4 with them */

		assertEquals(4, distinctVertices(model).size());

	}

	@Test
	public void testGeometryIsPreserved() throws IOException {

		/* two triangles which do not share any vertex, so nothing can be merged */

		List<TriangleXYZ> input = List.of(
				new TriangleXYZ(new VectorXYZ(0, 0, 0), new VectorXYZ(1, 0, 0), new VectorXYZ(0, 0, 1)),
				new TriangleXYZ(new VectorXYZ(9, 0, 9), new VectorXYZ(8, 0, 9), new VectorXYZ(9, 0, 8)));

		GltfModel model = writeAndReadGltf(input);

		assertEquals(2, triangles(model).size());
		assertEquals(6, distinctVertices(model).size());

		/* each triangle still has the same shape, i.e. the indices refer to the right vertices. */

		assertEquals(sideLengths(input), sideLengths(triangles(model)));

	}

	@Test
	public void testWindingOrderIsPreserved() throws IOException {

		/* a triangle whose vertices are not in the same order as its neighbor's,
		 * to check that indices do not reorder the vertices of a triangle */

		var t = new TriangleXYZ(new VectorXYZ(0, 0, 0), new VectorXYZ(2, 0, 0), new VectorXYZ(0, 0, 2));

		GltfModel model = writeAndReadGltf(List.of(t));

		TriangleXYZ result = triangles(model).get(0);

		assertEquals("the normal still points the same way",
				0, result.getNormal().distanceTo(t.getNormal()), 0.001);

	}

	@Test
	public void testManyTriangles() throws IOException {

		/* enough triangles that the vertices would not fit into an unsigned byte,
		 * but still few enough for unsigned short indices */

		List<TriangleXYZ> input = new ArrayList<>();
		for (int i = 0; i < 500; i++) {
			input.add(new TriangleXYZ(
					new VectorXYZ(i, 0, 0), new VectorXYZ(i + 1, 0, 0), new VectorXYZ(i, 0, 1)));
		}

		GltfModel model = writeAndReadGltf(input);

		assertEquals(500, triangles(model).size());

		/* neighboring triangles share the vertex at (i, 0, 0) */

		assertTrue("vertices are shared between triangles: " + distinctVertices(model).size(),
				distinctVertices(model).size() < 3 * 500);

	}

	private static List<TriangleXYZ> triangles(GltfModel model) {
		List<TriangleXYZ> result = new ArrayList<>();
		model.getMeshes().forEach(m -> result.addAll(m.geometry.asTriangles().triangles));
		return result;
	}

	private static Set<VectorXYZ> distinctVertices(GltfModel model) {
		return triangles(model).stream().flatMap(t -> t.verticesNoDup().stream()).collect(toSet());
	}

	/** writes the triangles to a temporary glTF file with {@link GltfOutput} and reads them back */
	private static GltfModel writeAndReadGltf(List<TriangleXYZ> triangles) throws IOException {

		Material material = new Material(FLAT, Color.WHITE);

		var builder = new TriangleGeometry.Builder(material.textureLayers().size(), null, FLAT);
		builder.addTriangles(triangles);
		Mesh mesh = new Mesh(builder.build(), material);

		File tempFile = writeGltf(".gltf", List.of(mesh), Map.of());

		return GltfModel.loadFromFile(tempFile);

	}

}
