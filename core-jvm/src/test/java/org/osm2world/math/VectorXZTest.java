package org.osm2world.math;

import static java.lang.Math.PI;
import static org.osm2world.math.VectorXZ.*;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import org.junit.Test;

public class VectorXZTest {

	@Test
	public void testRightNormal() {

		VectorXZ xUnitRightNormal = VectorXZ.X_UNIT.rightNormal();
		assertAlmostEquals(0, -1, xUnitRightNormal);

		VectorXZ testA = new VectorXZ(0.5f, 0.5f);
		VectorXZ testARightNormal = testA.rightNormal();
		assertAlmostEquals(0.707f, -0.707f, testARightNormal);

	}

	@Test
	public void testAngle() {

		assertAlmostEquals(     0, new VectorXZ( 0, +1).angle());
		assertAlmostEquals(0.5*PI, new VectorXZ(+1,  0).angle());
		assertAlmostEquals(    PI, new VectorXZ( 0, -1).angle());
		assertAlmostEquals(1.5*PI, new VectorXZ(-1,  0).angle());

		assertAlmostEquals(0.25*PI, new VectorXZ(1, 1).angle());
		assertAlmostEquals(0.25*PI, new VectorXZ(5, 5).angle());

	}

	@Test
	public void testFromAngle() {

		assertAlmostEquals( 0, +1, fromAngle(0));
		assertAlmostEquals(+1,  0, fromAngle(0.5*PI));
		assertAlmostEquals( 0, -1, fromAngle(PI));
		assertAlmostEquals(-1,  0, fromAngle(1.5*PI));

	}

	@Test
	public void testAngleBetween() {

		assertAlmostEquals(       0, angleBetween(X_UNIT, X_UNIT));
		assertAlmostEquals(       0, angleBetween(Z_UNIT, Z_UNIT));
		assertAlmostEquals(      PI, angleBetween(X_UNIT, X_UNIT.invert()));
		assertAlmostEquals(      PI, angleBetween(Z_UNIT, Z_UNIT.invert()));
		assertAlmostEquals(0.5 * PI, angleBetween(X_UNIT, Z_UNIT));
		assertAlmostEquals(0.5 * PI, angleBetween(Z_UNIT, X_UNIT));
		assertAlmostEquals(0.5 * PI, angleBetween(Z_UNIT, X_UNIT.mult(3)));

		assertAlmostEquals(0.25 * PI, angleBetween(
				X_UNIT, new VectorXZ(1, 1)));
		assertAlmostEquals(0.75 * PI, angleBetween(
				X_UNIT, new VectorXZ(-1, -1)));

	}

	@Test
	public void testClockwiseAngleBetween() {

		assertAlmostEquals(       0, clockwiseAngleBetween(X_UNIT, X_UNIT));
		assertAlmostEquals(       0, clockwiseAngleBetween(Z_UNIT, Z_UNIT));
		assertAlmostEquals(      PI, clockwiseAngleBetween(X_UNIT, X_UNIT.invert()));
		assertAlmostEquals(      PI, clockwiseAngleBetween(Z_UNIT, Z_UNIT.invert()));
		assertAlmostEquals(1.5 * PI, clockwiseAngleBetween(X_UNIT, Z_UNIT));
		assertAlmostEquals(0.5 * PI, clockwiseAngleBetween(Z_UNIT, X_UNIT));
		assertAlmostEquals(0.5 * PI, clockwiseAngleBetween(Z_UNIT, X_UNIT.mult(3)));

		assertAlmostEquals(1.75 * PI, clockwiseAngleBetween(X_UNIT, new VectorXZ(1, 1)));
		assertAlmostEquals(0.25 * PI, clockwiseAngleBetween(new VectorXZ(1, 1), X_UNIT));

		assertAlmostEquals(0.75 * PI, clockwiseAngleBetween(X_UNIT, new VectorXZ(-1, -1)));
		assertAlmostEquals(1.25 * PI, clockwiseAngleBetween(new VectorXZ(-1, -1), X_UNIT));

	}

	@Test
	public void testRotate() {

		assertAlmostEquals(new VectorXZ( 0,  0), new VectorXZ( 0,   0).rotate(42));
		assertAlmostEquals(new VectorXZ( 2,  5), new VectorXZ( 2,   5).rotate(PI*2));

		assertAlmostEquals(new VectorXZ( 2, -1), new VectorXZ( 1,   2).rotate(+PI/2));
		assertAlmostEquals(new VectorXZ(-2,  1), new VectorXZ( 1,   2).rotate(-PI/2));

		assertAlmostEquals(new VectorXZ( 8, 42), new VectorXZ(-8, -42).rotate(-PI));
		assertAlmostEquals(new VectorXZ( 8, 42), new VectorXZ(-8, -42).rotate(+PI));

	}

	@Test
	public void testMirrorX() {

		assertAlmostEquals(-25, 0, new VectorXZ(25, 0).mirrorX(0));
		assertAlmostEquals(0, 0, new VectorXZ(0, 0).mirrorX(0));
		assertAlmostEquals(-3, 0, new VectorXZ(-3, 0).mirrorX(-3));
		assertAlmostEquals(5, 0, new VectorXZ(0, 0).mirrorX(2.5));

	}

}
