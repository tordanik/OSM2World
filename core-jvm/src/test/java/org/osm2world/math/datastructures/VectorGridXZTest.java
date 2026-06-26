package org.osm2world.math.datastructures;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;

public class VectorGridXZTest {

	private final VectorGridXZ grid = new VectorGridXZ(
			new AxisAlignedRectangleXZ(-5, 0, 5, 5), 1);

	@Test
	public void testSize() {

		assertEquals(11, grid.sizeX());
		assertEquals(6, grid.sizeZ());
		assertEquals(66, grid.size());

	}

	@Test
	public void testGridLines() {

		assertEquals(85, grid.gridLines().size());

	}

}
