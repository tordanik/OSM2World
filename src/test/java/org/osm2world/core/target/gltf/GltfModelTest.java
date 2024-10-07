package org.osm2world.core.target.gltf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.osm2world.core.target.gltf.GltfModel.readComponent;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Ignore;
import org.junit.Test;
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

	@Ignore
	@Test
	public void testLoadFromFile_BoxVertexColors() throws IOException {

		String assetName = "BoxVertexColors";
		String fileName = "gltf" + File.separator + assetName + File.separator + "BoxVertexColors.gltf";

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File testFile = new File(classLoader.getResource(fileName).getFile());

		var model = GltfModel.loadFromFile(testFile);
		var meshes = model.buildMeshes(new InstanceParameters(VectorXYZ.NULL_VECTOR, 0));

		assertFalse(meshes.isEmpty());

	}

}