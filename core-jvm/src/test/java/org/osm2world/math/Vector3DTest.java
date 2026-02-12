package org.osm2world.math;

import static org.junit.Assert.assertEquals;
import static org.osm2world.math.Vector3D.withoutNegativeZero;

import org.junit.Test;

public class Vector3DTest {

	@Test
	public void testWithoutNegativeZero() {

		assertEquals(new VectorXYZ(0, 1, 0), withoutNegativeZero(new VectorXYZ(-0.0, 1.0, -0.0)));
		assertEquals(new VectorXZ(0, 0), withoutNegativeZero(new VectorXZ(0.0, -0.0)));

	}

}