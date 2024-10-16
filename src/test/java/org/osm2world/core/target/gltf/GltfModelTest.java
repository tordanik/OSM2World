package org.osm2world.core.target.gltf;

import static org.junit.Assert.assertEquals;
import static org.osm2world.core.target.gltf.GltfModel.readComponent;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.junit.Test;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.model.InstanceParameters;
import org.osm2world.core.target.gltf.data.GltfAccessor;

public class GltfModelTest {

	@Test
	public void testReadComponent() {

		ByteBuffer b = ByteBuffer.allocate(1000);
		b.put((byte)-42);
		b.put((byte)42);
		b.put((byte)(128 + 42));
		b.putShort((short)-4242);
		b.putShort((short)4242);
		b.putInt(42424242);
		b.putFloat(-42.3f);
		b.rewind();

		assertEquals(-42, readComponent(b, GltfAccessor.TYPE_BYTE, false), 0.001f);
		assertEquals(42, readComponent(b, GltfAccessor.TYPE_UNSIGNED_BYTE, false), 0.001f);
		assertEquals(170, readComponent(b, GltfAccessor.TYPE_UNSIGNED_BYTE, false), 0.001f);
		assertEquals(-4242, readComponent(b, GltfAccessor.TYPE_SHORT, false), 0.001f);
		assertEquals(4242, readComponent(b, GltfAccessor.TYPE_UNSIGNED_SHORT, false), 0.001f);
		assertEquals(42424242, readComponent(b, GltfAccessor.TYPE_UNSIGNED_INT, false), 0.001f);
		assertEquals(-42.3f, readComponent(b, GltfAccessor.TYPE_FLOAT, false), 0.001f);

	}

	@Test
	public void testLoadFromFile_BoxVertexColors() throws IOException {

		for (String extension : List.of(".gltf", "_embedded.gltf")) {

			var model = loadGltfTestModel("BoxVertexColors", extension);
			var meshes = model.buildMeshes(new InstanceParameters(VectorXYZ.NULL_VECTOR, 0));

			assertEquals(1, meshes.size());
			assertEquals(12, meshes.get(0).geometry.asTriangles().triangles.size());

		}

	}

	@Test
	public void testLoadFromFile_Triangle() throws IOException {

		for (String assetName : List.of("Triangle", "TriangleWithoutIndices")) {
			for (String extension : List.of(".gltf", "_embedded.gltf")) {

				var model = loadGltfTestModel(assetName, extension);
				var meshes = model.buildMeshes(new InstanceParameters(VectorXYZ.NULL_VECTOR, 0));

				assertEquals(1, meshes.size());
				assertEquals(1, meshes.get(0).geometry.asTriangles().triangles.size());

			}
		}

	}

	@Test
	public void testLoadFromFile_SimpleMeshes() throws IOException {

		var model = loadGltfTestModel("SimpleMeshes", ".gltf");
		var meshes = model.buildMeshes(new InstanceParameters(VectorXYZ.NULL_VECTOR, 0));

		assertEquals(2, meshes.size());

		List<TriangleXYZ> triangles1 = meshes.get(0).geometry.asTriangles().triangles;
		List<TriangleXYZ> triangles2 = meshes.get(1).geometry.asTriangles().triangles;

		assertEquals(1, triangles1.size());
		assertEquals(1, triangles2.size());

		assertEquals(1.0, triangles1.get(0).getCenter().distanceTo(triangles2.get(0).getCenter()), 0.001);

	}

	private static GltfModel loadGltfTestModel(String assetName, String extension) throws IOException {

		String fileName = "gltf" + File.separator + assetName + File.separator + assetName + extension;

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File testFile = new File(classLoader.getResource(fileName).getFile());

		return GltfModel.loadFromFile(testFile);

	}

}