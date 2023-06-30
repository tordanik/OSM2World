package org.osm2world.core.target.gltf;

import static org.osm2world.core.math.VectorXYZ.NULL_VECTOR;
import static org.osm2world.core.target.gltf.GltfTarget.Compression.NONE;
import static org.osm2world.core.target.gltf.GltfTarget.Compression.ZIP;
import static org.osm2world.core.target.gltf.GltfTarget.GltfFlavor.GLB;
import static org.osm2world.core.target.gltf.GltfTarget.GltfFlavor.GLTF;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.osm2world.core.target.common.material.Materials;

public class GltfTargetTest {

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

	private static void createTemporaryTestGltf(String fileExtension) throws IOException {

		File tempFile = File.createTempFile("osm2world-test-", fileExtension);
		tempFile.deleteOnExit();

		var target = switch (fileExtension) {
			case ".gltf" -> new GltfTarget(tempFile, GLTF, NONE, null);
			case ".glb" -> new GltfTarget(tempFile, GLB, NONE, null);
			case ".gltf.gz" -> new GltfTarget(tempFile, GLTF, ZIP, null);
			case ".glb.gz" -> new GltfTarget(tempFile, GLB, ZIP, null);
			default -> throw new Error("unsupported extension: " + fileExtension);
		};

		target.drawColumn(Materials.STEEL, null, NULL_VECTOR, 10, 2,0, true, false);
		target.finish();

	}

}
