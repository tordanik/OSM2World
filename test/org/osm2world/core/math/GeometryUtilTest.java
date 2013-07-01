package org.osm2world.core.math;

import static junit.framework.Assert.*;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.math.VectorXZ.*;
import static org.osm2world.core.test.TestUtil.*;

import java.util.List;

import org.junit.Test;

public class GeometryUtilTest {

	@Test
	public void testIsRightOf() {
		
		assertTrue(isRightOf(X_UNIT, NULL_VECTOR, Z_UNIT));
		assertFalse(isRightOf(X_UNIT, Z_UNIT, NULL_VECTOR));

		assertTrue(isRightOf(NULL_VECTOR, Z_UNIT, X_UNIT));
		assertFalse(isRightOf(NULL_VECTOR, X_UNIT, Z_UNIT));
		
		for (VectorXZ v1 : anyVectorXZ()) {
			for (VectorXZ v2 : anyVectorXZ()) {
			
				if (!v1.equals(v2) && !v2.equals(NULL_VECTOR)) {
					
					VectorXZ l1 = v1;
					VectorXZ l2 = v1.add(v2);
					VectorXZ pR = v1.add(v2.rightNormal());
					VectorXZ pL = v1.subtract(v2.rightNormal());
					
					assertTrue(pR + " should be right of " + l1 + "-" + "l2",
							isRightOf(pR, l1, l2));
					assertFalse(pL + " should not be right of " + l1 + "-" + "l2",
							isRightOf(pL, l1, l2));
					
				}
				
			}
		}
		
	}
	
	@Test
	public void testIsBetween() {
		
		assertTrue(isBetween(NULL_VECTOR, X_UNIT, X_UNIT.invert()));
		assertTrue(isBetween(NULL_VECTOR, X_UNIT.invert(), X_UNIT));
		assertTrue(isBetween(Z_UNIT, X_UNIT.invert(), X_UNIT));
		
	}
	
	@Test
	public void testInterpolateElevation() {
		
		assertEquals(9.0, GeometryUtil.interpolateElevation(
				new VectorXZ(5, 1),
				new VectorXYZ(3, 7, 1),
				new VectorXYZ(6, 10, 1)).y);
		
	}
	
	@Test
	public void testEquallyDistributePointsAlong1StartEnd() {
		
		List<VectorXZ> result = GeometryUtil.equallyDistributePointsAlong(
				1f, true, new VectorXZ(-2, 5), new VectorXZ(+4, 5));
		
		assertSame(7, result.size());
		assertAlmostEquals(-2, 5, result.get(0));
		assertAlmostEquals(-1, 5, result.get(1));
		assertAlmostEquals( 0, 5, result.get(2));
		assertAlmostEquals(+1, 5, result.get(3));
		assertAlmostEquals(+2, 5, result.get(4));
		assertAlmostEquals(+3, 5, result.get(5));
		assertAlmostEquals(+4, 5, result.get(6));
		
	}

	@Test
	public void testEquallyDistributePointsAlong1NoStartEnd() {
		
		List<VectorXZ> result = GeometryUtil.equallyDistributePointsAlong(
				1f, false, new VectorXZ(-2, 5), new VectorXZ(+4, 5));
		
		assertSame(6, result.size());
		assertAlmostEquals(-1.5f, 5, result.get(0));
		assertAlmostEquals(-0.5f, 5, result.get(1));
		assertAlmostEquals(+0.5f, 5, result.get(2));
		assertAlmostEquals(+1.5f, 5, result.get(3));
		assertAlmostEquals(+2.5f, 5, result.get(4));
		assertAlmostEquals(+3.5f, 5, result.get(5));
		
	}
}
