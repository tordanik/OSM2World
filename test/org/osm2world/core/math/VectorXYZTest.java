package org.osm2world.core.math;

import org.junit.Test;

public class VectorXYZTest {
	
	@Test
	public void testRotateX() {
		
		assertVectorEpsilon(1, 0, 0, VectorXYZ.X_UNIT.rotateX(-0.42));
		assertVectorEpsilon(0, 0, 1, VectorXYZ.Y_UNIT.rotateX(Math.PI/2));
		assertVectorEpsilon(0,-1, 0, VectorXYZ.Z_UNIT.rotateX(Math.PI/2));
		
	}
	
	@Test
	public void testRotateY() {
		
		assertVectorEpsilon(0, 0, 1, VectorXYZ.X_UNIT.rotateY(-Math.PI/2));
		assertVectorEpsilon(0, 1, 0, VectorXYZ.Y_UNIT.rotateY(Math.PI*0.42));
		assertVectorEpsilon(1, 0, 0, VectorXYZ.Z_UNIT.rotateY(Math.PI/2));
		
	}
	
	@Test
	public void testRotateZ() {
		
		assertVectorEpsilon(0, 1, 0, VectorXYZ.X_UNIT.rotateZ(Math.PI/2));
		assertVectorEpsilon(0,-1, 0, VectorXYZ.Y_UNIT.rotateZ(Math.PI));
		assertVectorEpsilon(0, 0, 1, VectorXYZ.Z_UNIT.rotateZ(0.42));
	}
	
	private static final float EPSILON = 0.01f;
	
	private static void assertEpsilon(float expected, float actual) {
		if (Math.abs(expected - actual) > EPSILON) {
			throw new AssertionError("expected " + expected + ", was " + actual);
		}
	}
	
	private static void assertVectorEpsilon(float x, float y, float z, VectorXYZ vector) {
		if (Math.abs(x - vector.x) > EPSILON) {
			throw new AssertionError("expected x=" + x + ", was " + vector.x);
		}
		if (Math.abs(y - vector.y) > EPSILON) {
			throw new AssertionError("expected y=" + y + ", was " + vector.y);
		}
		if (Math.abs(z - vector.z) > EPSILON) {
			throw new AssertionError("expected z=" + z + ", was " + vector.z);
		}
	}
}
