package org.osm2world.output.gltf;

import static org.osm2world.math.VectorXYZ.NULL_VECTOR;
import static org.osm2world.util.test.TestFileUtil.createTempFile;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.scene.Scene;
import org.osm2world.scene.material.Materials;
import org.osm2world.scene.mesh.ExtrusionGeometry;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.test.TestWorldModule;

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

		File tempFile = createTempFile(fileExtension);

		var mesh = new Mesh(ExtrusionGeometry.createColumn(
				null, NULL_VECTOR, 10, 2, 0, true, false, null,
						Materials.STEEL.get(config).textureDimensions()), Materials.STEEL.get(config));

		MapDataBuilder dataBuilder = new MapDataBuilder();
		MapNode node = dataBuilder.createNode(0, 0);
		node.addRepresentation(new TestWorldModule.TestNodeWorldObject(node, mesh));

		Scene scene = new Scene(null, dataBuilder.build());

		var target = new GltfOutput(tempFile);
		target.outputScene(scene);

	}

}
