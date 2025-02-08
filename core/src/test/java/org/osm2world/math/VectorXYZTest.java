package org.osm2world.math;

import static org.osm2world.math.VectorXYZ.*;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import org.junit.Test;

public class VectorXYZTest {

	@Test
	public void testRotateX() {

		assertAlmostEquals(1, 0, 0, X_UNIT.rotateX(-0.42));
		assertAlmostEquals(0, 0, 1, Y_UNIT.rotateX(Math.PI/2));
		assertAlmostEquals(0,-1, 0, Z_UNIT.rotateX(Math.PI/2));

	}

	@Test
	public void testRotateY() {

		assertAlmostEquals(0, 0, 1, X_UNIT.rotateY(-Math.PI/2));
		assertAlmostEquals(0, 1, 0, Y_UNIT.rotateY(Math.PI*0.42));
		assertAlmostEquals(1, 0, 0, Z_UNIT.rotateY(Math.PI/2));

	}

	@Test
	public void testRotateZ() {

		assertAlmostEquals(0, 1, 0, X_UNIT.rotateZ(Math.PI/2));
		assertAlmostEquals(0,-1, 0, Y_UNIT.rotateZ(Math.PI));
		assertAlmostEquals(0, 0, 1, Z_UNIT.rotateZ(0.42));

	}

}
