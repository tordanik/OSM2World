package org.osm2world.output.frontend_pbf;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osm2world.math.VectorXZ.*;
import static org.osm2world.util.test.TestFileUtil.createTempFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;
import org.osm2world.O2WConverter;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.MapData;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.output.common.compression.Compression;
import org.osm2world.output.frontend_pbf.FrontendPbf.Tile;
import org.osm2world.output.frontend_pbf.FrontendPbf.WorldObject;
import org.osm2world.output.frontend_pbf.FrontendPbfOutput.Block;
import org.osm2world.output.frontend_pbf.FrontendPbfOutput.SimpleBlock;
import org.osm2world.output.frontend_pbf.FrontendPbfOutput.VectorBlock;
import org.osm2world.scene.Scene;
import org.osm2world.test.TestWorldModule;

public class FrontendPbfOutputTest {

	void testBlock(Block<VectorXZ> block) {

		List<VectorXZ> testVectors = asList(
				new VectorXZ(5, 22.2), NULL_VECTOR, X_UNIT, Z_UNIT);

		assertEquals(0, block.toIndex(testVectors.get(0)));

		assertEquals(1, block.toIndex(testVectors.get(1)));
		assertEquals(1, block.toIndex(testVectors.get(1)));

		assertEquals(2, block.toIndex(testVectors.get(2)));

		assertEquals(0, block.toIndex(testVectors.get(0)));

		assertEquals(3, block.toIndex(testVectors.get(3)));

		assertTrue(CollectionUtils.isEqualCollection(testVectors, block.getElements()));

	}

	@Test
	public void testSimpleBlock() {
		testBlock(new SimpleBlock<>());
	}

	@Test
	public void testVectorBlock() {
		testBlock(new VectorBlock<>());
	}

	@Test
	public void testWritePbfFile_empty() throws IOException {

		AxisAlignedRectangleXZ bbox = new AxisAlignedRectangleXZ(-1, -1, +1, +1);

		var mapDataBuilder = new MapDataBuilder();
		MapNode node = mapDataBuilder.createNode(0, 0);
		MapData mapData = mapDataBuilder.build();
		node.addRepresentation(new TestWorldModule.TestNodeWorldObject(node));
		Scene testScene = new Scene(null, mapData);

		File outputFile = createTempFile(".o2w.pbf");

		var output = new FrontendPbfOutput(outputFile, Compression.NONE, bbox);
		output.outputScene(testScene);

	}

	@Test
	public void testWritePbfFile_extrusion() throws IOException {

		AxisAlignedRectangleXZ bbox = new AxisAlignedRectangleXZ(-1, -1, +1, +1);

		var mapDataBuilder = new MapDataBuilder();
		mapDataBuilder.createNode(0, 0, TagSet.of("highway", "street_lamp"));

		Scene results = new O2WConverter().convert(mapDataBuilder.build(), null);

		File outputFile = createTempFile(".o2w.pbf");

		var output = new FrontendPbfOutput(outputFile, Compression.NONE, bbox);
		output.outputScene(results);

		try (FileInputStream is = new FileInputStream(outputFile)) {

			Tile tile = Tile.parseFrom(is, null);

			Optional<WorldObject> lampObject = tile.getObjectsList().stream()
					.filter(o -> "n0".equals(o.getOsmId())).findAny();
			assertTrue(lampObject.isPresent());
			assertTrue(lampObject.get().getExtrusionGeometriesCount() > 0);

		}

	}

}
