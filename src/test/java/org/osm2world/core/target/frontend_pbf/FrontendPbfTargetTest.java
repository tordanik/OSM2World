package org.osm2world.core.target.frontend_pbf;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.osm2world.core.math.VectorXZ.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.osm2world.core.ConversionFacade;
import org.osm2world.core.ConversionFacade.BoundingBoxSizeException;
import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.target.frontend_pbf.FrontendPbfTarget.Block;
import org.osm2world.core.target.frontend_pbf.FrontendPbfTarget.SimpleBlock;
import org.osm2world.core.target.frontend_pbf.FrontendPbfTarget.VectorBlock;
import org.osm2world.core.test.TestWorldModule;

import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.impl.Node;

public class FrontendPbfTargetTest {

	public void testBlock(Block<VectorXZ> block) {

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
		testBlock(new SimpleBlock<VectorXZ>());
	}

	@Test
	public void testVectorBlock() {
		testBlock(new VectorBlock<VectorXZ>());
	}

	@Test
	public void testWritePbfFile() throws BoundingBoxSizeException, IOException {

		AxisAlignedRectangleXZ bbox = new AxisAlignedRectangleXZ(-1, -1, +1, +1);

		OsmNode node = new Node(0, 0, 0);
		OSMData osmData = new OSMData(emptyList(), asList(node), emptyList(), emptyList());

		ConversionFacade cf = new ConversionFacade();
		Results results = cf.createRepresentations(osmData, asList(new TestWorldModule()), null, null);

		File outputFile = File.createTempFile("unittest", ".o2w.pbf");
		outputFile.deleteOnExit();
		FrontendPbfTarget.writePbfFile(outputFile, results.getMapData(), bbox, null);

	}

}
