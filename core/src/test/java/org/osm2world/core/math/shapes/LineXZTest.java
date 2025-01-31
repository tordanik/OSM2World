package org.osm2world.core.math.shapes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import org.junit.Test;
import org.osm2world.core.math.VectorXZ;

public class LineXZTest {

	@Test
	public void testGetIntersection() {

		var line = new LineXZ(new VectorXZ(-1, -1), new VectorXZ(1, 1));

		var l1 = new LineSegmentXZ(new VectorXZ(1, -1), new VectorXZ(-1, 1));
		assertEquals(new VectorXZ(0, 0), line.getIntersection(l1));
		assertEquals(new VectorXZ(0, 0), line.getIntersection(l1.toLineXZ()));

		var l2 = new LineSegmentXZ(new VectorXZ(1, -1), new VectorXZ(0.5, -0.5));
		assertNull(line.getIntersection(l2));
		assertEquals(new VectorXZ(0, 0), line.getIntersection(l2.toLineXZ()));

	}

	@Test
	public void testGetIntersection2() {

		var line = new LineXZ(new VectorXZ(-10, 5), new VectorXZ(+10, 5));

		var l1 = new LineSegmentXZ(new VectorXZ(0, 10), new VectorXZ(0, 0));
		assertAlmostEquals(0, 5, line.getIntersection(l1));

		var l2 = new LineSegmentXZ(new VectorXZ(10, 0), new VectorXZ(0, 10));
		assertAlmostEquals(5, 5, line.getIntersection(l2));

	}

}