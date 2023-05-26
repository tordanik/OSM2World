package org.osm2world.core.target.gltf;

import static org.osm2world.core.math.VectorXYZ.NULL_VECTOR;
import static org.osm2world.core.target.gltf.GltfTarget.GltfFlavor.GLB;
import static org.osm2world.core.target.gltf.GltfTarget.GltfFlavor.GLTF;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.osm2world.core.target.common.material.Materials;

public class GltfTargetTest {

	@Test
	public void testSimpleGltf() throws IOException {

		File tempFile = File.createTempFile("osm2world-test-", ".gltf");
		tempFile.deleteOnExit();

		var target = new GltfTarget(tempFile, GLTF, null);
		target.drawColumn(Materials.STEEL, null, NULL_VECTOR, 10, 2,0, true, false);
		target.finish();

	}

	@Test
	public void testSimpleGlb() throws IOException {

		File tempFile = File.createTempFile("osm2world-test-", ".glb");
		tempFile.deleteOnExit();

		var target = new GltfTarget(tempFile, GLB, null);
		target.drawColumn(Materials.STEEL, null, NULL_VECTOR, 10, 2,0, true, false);
		target.finish();

	}

}
