package org.osm2world.core.math;

import static java.lang.Math.PI;
import static org.osm2world.core.math.VectorXZ.*;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

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
		
	}
	
}
