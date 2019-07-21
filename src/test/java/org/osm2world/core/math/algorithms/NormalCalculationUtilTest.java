package org.osm2world.core.math.algorithms;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.osm2world.core.math.VectorXYZ.*;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import java.util.List;

import org.junit.Test;
import org.osm2world.core.math.VectorXYZ;


public class NormalCalculationUtilTest {

	@Test
	public final void testCalculateTriangleStripNormals() {

		List<VectorXYZ> vs = asList(
				NULL_VECTOR,
				new VectorXYZ(1, 0, -1),
				new VectorXYZ(1, 0, 0),
				new VectorXYZ(1, 1, 0)
				);

		List<VectorXYZ> normals =
				NormalCalculationUtil.calculateTriangleStripNormals(vs, true);

		assertEquals(4, normals.size());
		assertAlmostEquals(Y_UNIT, normals.get(2));
		assertAlmostEquals(X_UNIT.invert(), normals.get(3));

	}

	@Test
	public final void testCalculateTriangleFanNormals() {

		List<VectorXYZ> vs = asList(
				NULL_VECTOR,
				new VectorXYZ(1, 0, -1),
				new VectorXYZ(1, 0, 0),
				new VectorXYZ(1, 1, 0)
				);

		List<VectorXYZ> normals =
				NormalCalculationUtil.calculateTriangleFanNormals(vs, true);

		assertEquals(4, normals.size());
		assertAlmostEquals(Y_UNIT, normals.get(2));
		assertAlmostEquals(Z_UNIT.invert(), normals.get(3));

	}

}
