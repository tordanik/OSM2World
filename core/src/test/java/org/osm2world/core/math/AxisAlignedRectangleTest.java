package org.osm2world.core.math;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.osm2world.core.math.AxisAlignedRectangleXZ.bboxUnion;

import org.junit.Test;

public class AxisAlignedRectangleTest {

	@Test
	public void testBboxUnion() {

		AxisAlignedRectangleXZ box1 = new AxisAlignedRectangleXZ(5, 5, 10, 10);
		AxisAlignedRectangleXZ box2 = new AxisAlignedRectangleXZ(7, 3, 20, 10);

		AxisAlignedRectangleXZ result = bboxUnion(asList(box1, box2));

		assertEquals(5, result.minX, 0);
		assertEquals(3, result.minZ, 0);
		assertEquals(20, result.maxX, 0);
		assertEquals(10, result.maxZ, 0);

	}

}
