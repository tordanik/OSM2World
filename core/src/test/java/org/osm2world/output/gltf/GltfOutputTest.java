package org.osm2world.output.gltf;

import static org.osm2world.math.VectorXYZ.NULL_VECTOR;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.osm2world.output.common.material.Materials;

public class GltfOutputTest {

	@Test
	public void testSimpleGltf() throws IOException {
		createTemporaryTestGltf(".gltf");
	}

	@Test
	public void testSimpleGlb() throws IOException {
		createTemporaryTestGltf(".glb");
	}

	@Test
	public void testSimpleGltfGz() throws IOException {
		createTemporaryTestGltf(".gltf.gz");
	}

	@Test
	public void testSimpleGlbGz() throws IOException {
		createTemporaryTestGltf(".glb.gz");
	}

	@Test
	public void testSimpleGltfZip() throws IOException {
		createTemporaryTestGltf(".gltf.zip");
	}

	@Test
	public void testSimpleGlbZip() throws IOException {
		createTemporaryTestGltf(".glb.zip");
	}

	private static void createTemporaryTestGltf(String fileExtension) throws IOException {

		File tempFile = File.createTempFile("osm2world-test-", fileExtension);
		tempFile.deleteOnExit();

		var target = new GltfOutput(tempFile);

		target.drawColumn(Materials.STEEL, null, NULL_VECTOR, 10, 2,0, true, false);
		target.finish();

	}

}
