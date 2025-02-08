package org.osm2world.math.shapes;

import static org.osm2world.test.TestUtil.assertAlmostEquals;

import org.junit.Test;
import org.osm2world.math.VectorXZ;

public class LineSegmentXZTest {

	@Test
	public void testGetCenter() {

		assertAlmostEquals(-5, 0, new LineSegmentXZ(new VectorXZ(-10, 0), new VectorXZ(0, 0)).getCenter());
		assertAlmostEquals(-5, 9, new LineSegmentXZ(new VectorXZ(-10, 9), new VectorXZ(0, 9)).getCenter());
		assertAlmostEquals(0, 0, new LineSegmentXZ(new VectorXZ(-3, -3), new VectorXZ(3, 3)).getCenter());
		assertAlmostEquals(0, 0, new LineSegmentXZ(new VectorXZ(3, 3), new VectorXZ(-3, -3)).getCenter());

	}

	@Test
	public void testEvaluateAtX() {

		LineSegmentXZ segment = new LineSegmentXZ(new VectorXZ(-5, -10),  new VectorXZ(+5, +10));

		assertAlmostEquals(-10, segment.evaluateAtX(-5));
		assertAlmostEquals(-10, segment.reverse().evaluateAtX(-5));

		assertAlmostEquals(-4, segment.evaluateAtX(-2));
		assertAlmostEquals(-4, segment.reverse().evaluateAtX(-2));

		assertAlmostEquals(0, segment.evaluateAtX(0));
		assertAlmostEquals(0, segment.reverse().evaluateAtX(0));

		assertAlmostEquals(+10, segment.evaluateAtX(+5));
		assertAlmostEquals(+10, segment.reverse().evaluateAtX(+5));

	}

	@Test
	public void testEvaluateAtX_vertical() {

		LineSegmentXZ segment = new LineSegmentXZ(new VectorXZ(-5, -10),  new VectorXZ(-5, +10));

		assertAlmostEquals(-10, segment.evaluateAtX(-5));

		assertAlmostEquals(+10, segment.reverse().evaluateAtX(-5));

	}

}
