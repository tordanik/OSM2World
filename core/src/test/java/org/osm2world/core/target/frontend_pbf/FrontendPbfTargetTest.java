package org.osm2world.core.target.frontend_pbf;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osm2world.core.math.VectorXZ.*;
import static org.osm2world.core.target.TargetUtil.Compression;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;
import org.osm2world.core.ConversionFacade;
import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.map_data.creation.MapDataBuilder;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Tile;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.WorldObject;
import org.osm2world.core.target.frontend_pbf.FrontendPbfTarget.Block;
import org.osm2world.core.target.frontend_pbf.FrontendPbfTarget.SimpleBlock;
import org.osm2world.core.target.frontend_pbf.FrontendPbfTarget.VectorBlock;
import org.osm2world.core.test.TestWorldModule;

public class FrontendPbfTargetTest {

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
		mapDataBuilder.createNode(0, 0);

		ConversionFacade cf = new ConversionFacade();
		Results results = cf.createRepresentations(null, mapDataBuilder.build(), List.of(new TestWorldModule()), null, null);

		File outputFile = File.createTempFile("unittest", ".o2w.pbf");
		outputFile.deleteOnExit();
		FrontendPbfTarget.writePbfFile(outputFile, results.getMapData(), bbox, null, Compression.NONE);

	}

	@Test
	public void testWritePbfFile_extrusion() throws IOException {

		AxisAlignedRectangleXZ bbox = new AxisAlignedRectangleXZ(-1, -1, +1, +1);

		var mapDataBuilder = new MapDataBuilder();
		mapDataBuilder.createNode(0, 0, TagSet.of("highway", "street_lamp"));

		ConversionFacade cf = new ConversionFacade();
		Results results = cf.createRepresentations(null, mapDataBuilder.build(), null, null, null);

		File outputFile = File.createTempFile("unittest", ".o2w.pbf");
		outputFile.deleteOnExit();
		FrontendPbfTarget.writePbfFile(outputFile, results.getMapData(), bbox, null, Compression.NONE);

		try (FileInputStream is = new FileInputStream(outputFile)) {

			Tile tile = Tile.parseFrom(is, null);

			Optional<WorldObject> lampObject = tile.getObjectsList().stream()
					.filter(o -> "n0".equals(o.getOsmId())).findAny();
			assertTrue(lampObject.isPresent());
			assertTrue(lampObject.get().getExtrusionGeometriesCount() > 0);

		}

	}

}
