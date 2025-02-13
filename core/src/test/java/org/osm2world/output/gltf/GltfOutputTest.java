package org.osm2world.output.gltf;

import static org.osm2world.math.VectorXYZ.NULL_VECTOR;
import static org.osm2world.output.OutputUtil.Compression.*;
import static org.osm2world.output.gltf.GltfOutput.GltfFlavor.GLB;
import static org.osm2world.output.gltf.GltfOutput.GltfFlavor.GLTF;

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

		var target = switch (fileExtension) {
			case ".gltf" -> new GltfOutput(tempFile, GLTF, NONE, null);
			case ".glb" -> new GltfOutput(tempFile, GLB, NONE, null);
			case ".gltf.gz" -> new GltfOutput(tempFile, GLTF, GZ, null);
			case ".glb.gz" -> new GltfOutput(tempFile, GLB, GZ, null);
			case ".gltf.zip" -> new GltfOutput(tempFile, GLTF, ZIP, null);
			case ".glb.zip" -> new GltfOutput(tempFile, GLB, ZIP, null);
			default -> throw new Error("unsupported extension: " + fileExtension);
		};

		target.drawColumn(Materials.STEEL, null, NULL_VECTOR, 10, 2,0, true, false);
		target.finish();

	}

}
