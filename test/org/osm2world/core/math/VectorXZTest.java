package org.osm2world.core.math;

import org.junit.Test;

public class VectorXZTest {
	
	@Test
	public void testRightNormal() {
		
		VectorXZ xUnitRightNormal = VectorXZ.X_UNIT.rightNormal();
		assertVectorEpsilon(0, -1, xUnitRightNormal);
		
		VectorXZ testA = new VectorXZ(0.5f, 0.5f);
		VectorXZ testARightNormal = testA.rightNormal();
		assertVectorEpsilon(0.707f, -0.707f, testARightNormal);
		
	}
	
	public static final float EPSILON = 0.01f;
	
	public static void assertEpsilon(float expected, float actual) {
		if (Math.abs(expected - actual) > EPSILON) {
			throw new AssertionError("expected " + expected + ", was " + actual);
		}
	}
	
	public static void assertVectorEpsilon(float x, float z, VectorXZ vector) {
		if (Math.abs(x - vector.x) > EPSILON) {
			throw new AssertionError("expected x=" + x + ", was " + vector.x);
		}
		if (Math.abs(z - vector.z) > EPSILON) {
			throw new AssertionError("expected z=" + z + ", was " + vector.z);
		}
	}
}
