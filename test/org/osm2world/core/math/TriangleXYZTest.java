package org.osm2world.core.math;

import static org.osm2world.core.math.VectorXYZ.*;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import org.junit.Test;


public class TriangleXYZTest {

	@Test
	public void testGetYAt() {
		
		TriangleXYZ t1 = new TriangleXYZ(X_UNIT, Z_UNIT, Y_UNIT);
		
		assertAlmostEquals(1, t1.getYAt(new VectorXZ(0, 0)));
		
		assertAlmostEquals(0, t1.getYAt(new VectorXZ(0, 1)));
		assertAlmostEquals(0, t1.getYAt(new VectorXZ(1, 0)));
		assertAlmostEquals(0, t1.getYAt(new VectorXZ(0.5, 0.5)));
		assertAlmostEquals(0, t1.getYAt(new VectorXZ(0.8, 0.2)));

		assertAlmostEquals(0.5, t1.getYAt(new VectorXZ(0, 0.5)));
		assertAlmostEquals(0.5, t1.getYAt(new VectorXZ(0.5, 0)));
		assertAlmostEquals(0.5, t1.getYAt(new VectorXZ(0.25, 0.25)));
		
	}
	
}
