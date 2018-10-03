package org.osm2world.core.target.frontend_pbf;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.osm2world.core.math.VectorXZ.*;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.frontend_pbf.FrontendPbfTarget.Block;

public class FrontendPbfTargetTest {

	@Test
	public void testBlock() {

		List<VectorXZ> testVectors = asList(
				new VectorXZ(5, 22.2), NULL_VECTOR, X_UNIT, Z_UNIT);

		Block<VectorXZ> block = new Block<VectorXZ>();

		assertEquals(0, block.toIndex(testVectors.get(0)));

		assertEquals(1, block.toIndex(testVectors.get(1)));
		assertEquals(1, block.toIndex(testVectors.get(1)));

		assertEquals(2, block.toIndex(testVectors.get(2)));

		assertEquals(0, block.toIndex(testVectors.get(0)));

		assertEquals(3, block.toIndex(testVectors.get(3)));

		assertTrue(CollectionUtils.isEqualCollection(testVectors, block.getElements()));

	}

}
