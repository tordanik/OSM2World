package org.osm2world.output.gltf;

import static org.osm2world.util.test.TestFileUtil.createTempFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.scene.Scene;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.util.test.TestWorldModule;

final class GltfOutputTestUtil {

	private GltfOutputTestUtil() {}

	static File writeGltf(String fileExtension, List<Mesh> meshes, Map<String, Object> configProperties) {

		File tempFile = createTempFile(fileExtension);

		MapDataBuilder dataBuilder = new MapDataBuilder();

		for (Mesh mesh : meshes) {
			MapNode node = dataBuilder.createNode(0, 0);
			node.addRepresentation(new TestWorldModule.TestNodeWorldObject(node, mesh));
		}

		Scene scene = new Scene(null, dataBuilder.build());

		var output = new GltfOutput(tempFile);
		output.setConfiguration(new O2WConfig(configProperties));
		output.outputScene(scene);

		return tempFile;

	}

	static @Nonnull String fileToString(File file) throws IOException {
		return Files.readString(file.toPath(), StandardCharsets.UTF_8).replaceAll("\\s", "");
	}

	/** the side lengths of each triangle, for comparing geometry regardless of placement or rotation */
	static List<Double> sideLengths(List<TriangleXYZ> triangles) {
		return triangles.stream()
				.flatMap(t -> Stream.of(
								t.v1.distanceTo(t.v2),
								t.v2.distanceTo(t.v3),
								t.v3.distanceTo(t.v1)))
						.map(d -> Math.round(d * 1000) / 1000.0) // avoid comparing floating point values exactly
						.sorted().toList();
	}

}
