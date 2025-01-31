package org.osm2world.core.math.datastructures;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.AxisAlignedRectangleXZ;

public class IndexGridTest {

	@Test
	public void testCellCoords() {

		IndexGrid<VectorXZ> grid = new IndexGrid<>(new AxisAlignedRectangleXZ(-10, -10, 10, 10), 2, 2);

		assertEquals(2, grid.getCellArray().length);
		assertEquals(2, grid.getCellArray()[0].length);

		assertEquals(0, grid.cellXForCoord(-20));
		assertEquals(0, grid.cellXForCoord(-5));
		assertEquals(1, grid.cellXForCoord(5));
		assertEquals(1, grid.cellXForCoord(20));

	}

}
