package org.osm2world.core.math.algorithms;

import static java.lang.Math.sqrt;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.osm2world.core.math.VectorXYZ.*;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import java.util.List;

import org.junit.Test;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.shapes.TriangleXYZ;


public class NormalCalculationUtilTest {

	@Test
	public final void testCalculateTriangleNormals() {

		List<TriangleXYZ> triangles = asList(
				new TriangleXYZ(X_UNIT, Z_UNIT, NULL_VECTOR),
				new TriangleXYZ(Y_UNIT, NULL_VECTOR, Z_UNIT));

		{
			List<VectorXYZ> normalsFlat = NormalCalculationUtil.calculateTriangleNormals(triangles, false);

			assertEquals(6, normalsFlat.size());
			assertAlmostEquals(Y_UNIT, normalsFlat.get(0));
			assertAlmostEquals(Y_UNIT, normalsFlat.get(1));
			assertAlmostEquals(Y_UNIT, normalsFlat.get(2));
			assertAlmostEquals(X_UNIT, normalsFlat.get(3));
			assertAlmostEquals(X_UNIT, normalsFlat.get(4));
			assertAlmostEquals(X_UNIT, normalsFlat.get(5));
		} {
			List<VectorXYZ> normalsSmooth = NormalCalculationUtil.calculateTriangleNormals(triangles, true);

			assertEquals(6, normalsSmooth.size());
			assertAlmostEquals(Y_UNIT, normalsSmooth.get(0));
			assertAlmostEquals(new VectorXYZ(1/sqrt(2), 1/sqrt(2), 0), normalsSmooth.get(1));
			assertAlmostEquals(new VectorXYZ(1/sqrt(2), 1/sqrt(2), 0), normalsSmooth.get(2));
			assertAlmostEquals(X_UNIT, normalsSmooth.get(3));
			assertAlmostEquals(new VectorXYZ(1/sqrt(2), 1/sqrt(2), 0), normalsSmooth.get(4));
			assertAlmostEquals(new VectorXYZ(1/sqrt(2), 1/sqrt(2), 0), normalsSmooth.get(5));
		}

	}

	@Test
	public final void testCalculateTriangleStripNormals() {

		List<VectorXYZ> vs = asList(
				NULL_VECTOR,
				new VectorXYZ(1, 0, -1),
				new VectorXYZ(1, 0, 0),
				new VectorXYZ(1, 1, 0)
				);

		{
			List<VectorXYZ> normalsFlat = NormalCalculationUtil.calculateTriangleStripNormals(vs, false);

			assertEquals(4, normalsFlat.size());
			assertAlmostEquals(Y_UNIT, normalsFlat.get(2));
			assertAlmostEquals(X_UNIT.invert(), normalsFlat.get(3));
		} {
			List<VectorXYZ> normalsSmooth = NormalCalculationUtil.calculateTriangleStripNormals(vs, true);

			assertEquals(4, normalsSmooth.size());
			assertAlmostEquals(Y_UNIT, normalsSmooth.get(0));
			assertAlmostEquals(new VectorXYZ(-1, 1, 0).normalize(), normalsSmooth.get(1));
			assertAlmostEquals(new VectorXYZ(-1, 1, 0).normalize(), normalsSmooth.get(2));
			assertAlmostEquals(X_UNIT.invert(), normalsSmooth.get(3));
		}

	}

	@Test
	public final void testCalculateTriangleFanNormals() {

		List<VectorXYZ> vs = asList(
				NULL_VECTOR,
				new VectorXYZ(1, 0, -1),
				new VectorXYZ(1, 0, 0),
				new VectorXYZ(1, 1, 0)
				);

		{
			List<VectorXYZ> normalsFlat = NormalCalculationUtil.calculateTriangleFanNormals(vs, false);

			assertEquals(4, normalsFlat.size());
			assertAlmostEquals(Y_UNIT, normalsFlat.get(2));
			assertAlmostEquals(Z_UNIT.invert(), normalsFlat.get(3));
		} {
			List<VectorXYZ> normalsSmooth = NormalCalculationUtil.calculateTriangleFanNormals(vs, true);

			assertEquals(4, normalsSmooth.size());
			assertAlmostEquals(new VectorXYZ(0, 1, -1).normalize(), normalsSmooth.get(0));
			assertAlmostEquals(Y_UNIT, normalsSmooth.get(1));
			assertAlmostEquals(new VectorXYZ(0, 1, -1).normalize(), normalsSmooth.get(2));
			assertAlmostEquals(Z_UNIT.invert(), normalsSmooth.get(3));
		}

	}

}
