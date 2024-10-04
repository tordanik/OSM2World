package org.osm2world.core.target.gltf;

import static org.junit.Assert.assertEquals;
import static org.osm2world.core.target.gltf.GltfModel.readComponent;

import java.nio.ByteBuffer;

import org.junit.Test;
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

}