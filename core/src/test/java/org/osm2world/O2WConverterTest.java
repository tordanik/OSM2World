package org.osm2world;

import static org.osm2world.output.common.compression.Compression.NONE;
import static org.osm2world.output.gltf.GltfOutput.GltfFlavor.GLTF;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.MapData;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.geo.MetricMapProjection;
import org.osm2world.output.Output;
import org.osm2world.output.gltf.GltfOutput;

public class O2WConverterTest {

	@Test
	public void testAreasWithDuplicateNodes() {

		List<TagSet> tagSets = List.of(TagSet.of("building", "yes"));

		for (TagSet tagSet : tagSets) {

			var builder = new MapDataBuilder();

			List<MapNode> nodes = List.of(
					builder.createNode(0, 0),
					builder.createNode(10, 0),
					builder.createNode(10, 5),
					builder.createNode(10, 5),
					builder.createNode(10, 10),
					builder.createNode(0, 10));
			builder.createWayArea(nodes, tagSet);

			MapData mapData = builder.build();

			try {

				File outputFile = Files.createTempFile("o2w-test-", ".gltf").toFile();
				Output testOutput = new GltfOutput(outputFile, GLTF, NONE, null);
				MapProjection mapProjection = new MetricMapProjection(new LatLon(0, 0));

				var o2w = new O2WConverter();
				o2w.convert(mapData, mapProjection, testOutput);

			} catch (Exception e) {
				throw new AssertionError("Conversion failed for tags: " +  tagSet, e);
			}

		}

	}

}
