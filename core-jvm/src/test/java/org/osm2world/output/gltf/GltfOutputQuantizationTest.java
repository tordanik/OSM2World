package org.osm2world.output.gltf;

import static org.junit.Assert.*;
import static org.osm2world.output.gltf.GltfOutput.KHR_MESH_QUANTIZATION;
import static org.osm2world.output.gltf.GltfOutputTestUtil.fileToString;
import static org.osm2world.output.gltf.GltfOutputTestUtil.sideLengths;
import static org.osm2world.scene.material.Material.Interpolation.FLAT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.output.gltf.data.GltfAccessor;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.mesh.TriangleGeometry;
import org.osm2world.util.platform.image.ImageImplementationJvm;
import org.osm2world.util.platform.json.JsonImplementationJvm;

/**
 * tests the use of the KHR_mesh_quantization extension by {@link GltfOutput}
 */
public class GltfOutputQuantizationTest {

	static {
		JsonImplementationJvm.register();
		ImageImplementationJvm.register();
	}

	/** a few triangles spread over an area of 1000 m, i.e. more than a single tile would cover */
	private static final List<TriangleXYZ> TRIANGLES = List.of(
			new TriangleXYZ(new VectorXYZ(0, 0, 0), new VectorXYZ(300, 0, 0), new VectorXYZ(0, 0, 400)),
			new TriangleXYZ(new VectorXYZ(-500, 20, -500), new VectorXYZ(500, 20, -500), new VectorXYZ(-500, 70, 500)));

	@Test
	public void testQuantizationAtLowLod() throws IOException {

		String json = fileToString(writeGltf(".gltf", 0, GltfOutput.KHR_MESH_QUANTIZATION));

		assertTrue("the extension is declared as used: " + json,
				json.contains("\"extensionsUsed\":[\"" + KHR_MESH_QUANTIZATION + "\"]"));
		assertTrue("the extension is declared as required: " + json,
				json.contains("\"extensionsRequired\":[\"" + KHR_MESH_QUANTIZATION + "\"]"));

		/* positions are 16-bit integers, normals are normalized 8-bit integers */

		assertTrue("positions use SHORT components: " + json,
				json.contains("\"componentType\":" + GltfAccessor.TYPE_SHORT));
		assertTrue("normals use BYTE components: " + json,
				json.contains("\"componentType\":" + GltfAccessor.TYPE_BYTE));
		assertTrue("normals are normalized: " + json, json.contains("\"normalized\":true"));

		/* the elements of quantized vertex attributes must be aligned to 4 byte boundaries */

		assertTrue("positions are padded to 8 bytes: " + json, json.contains("\"byteStride\":8"));
		assertTrue("normals are padded to 4 bytes: " + json, json.contains("\"byteStride\":4"));

		/* the root node undoes the quantization */

		assertTrue("the root node is scaled: " + json, json.contains("\"scale\":["));

	}

	@Test
	public void testNoQuantizationWithoutWhitelist() throws IOException {

		assertFalse(fileToString(writeGltf(".gltf", 0)).contains(KHR_MESH_QUANTIZATION));
		assertFalse(fileToString(writeGltf(".gltf", 0, "KHR_texture_transform")).contains(KHR_MESH_QUANTIZATION));

	}

	@Test
	public void testGeometryIsPreserved() throws IOException {

		File file = writeGltf(".glb", 0, KHR_MESH_QUANTIZATION);
		List<TriangleXYZ> result = trianglesOf(GltfModel.loadFromFile(file));

		assertEquals(TRIANGLES.size(), result.size());

		/* the triangles are compared by their side lengths because reading a file back in as a model.
		 * Positions are quantized to 1/65535 of the largest extent of the scene, which is 1000 m here. */

		List<Double> expectedSides = sideLengths(TRIANGLES);
		List<Double> actualSides = sideLengths(result);

		for (int i = 0; i < expectedSides.size(); i++) {
			assertEquals("side " + i + " of the quantized geometry",
					expectedSides.get(i), actualSides.get(i), 0.1);
		}

	}

	@Test
	public void testNormalsArePreserved() throws IOException {

		File file = writeGltf(".glb", 0, KHR_MESH_QUANTIZATION);

		/* both triangles are almost horizontal, so all normals must still point almost straight up
		 * despite being stored with only 8 bits per component */

		for (TriangleXYZ t : trianglesOf(GltfModel.loadFromFile(file))) {
			assertEquals("the triangle is still almost horizontal: " + t,
					1.0, Math.abs(t.getNormal().y), 0.01);
		}

	}

	private static List<TriangleXYZ> trianglesOf(GltfModel model) {
		List<TriangleXYZ> result = new ArrayList<>();
		model.getMeshes().forEach(m -> result.addAll(m.geometry.asTriangles().triangles));
		return result;
	}

	private static File writeGltf(String fileExtension, int lod, String... extensionWhitelist) {

		var properties = new HashMap<String, Object>(Map.of("lod", lod));
		if (extensionWhitelist.length > 0) {
			properties.put("gltfExtensionWhitelist", String.join(";", extensionWhitelist));
		}

		Material material = new Material(FLAT, Color.WHITE);
		var builder = new TriangleGeometry.Builder(material.textureLayers().size(), null, FLAT);
		builder.addTriangles(TRIANGLES);
		Mesh mesh = new Mesh(builder.build(), material);

		return GltfOutputTestUtil.writeGltf(fileExtension, List.of(mesh), properties);

	}

}
