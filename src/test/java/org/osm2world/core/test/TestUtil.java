package org.osm2world.core.test;

import static java.lang.Math.abs;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.SimplePolygonShapeXZ;

public final class TestUtil {

	private TestUtil() {}

	/**
	 * returns VectorXZ objects;
	 * can be used to test properties that need to be true for all vectors.
	 *
	 * Will contain some fixed common vectors
	 * and a lot of random vectors (that will be the same every time, though,
	 * so tests can be reproduced).
	 */
	public static final Iterable<VectorXZ> anyVectorXZ() {
		return Arrays.asList(
				VectorXZ.NULL_VECTOR,
				VectorXZ.X_UNIT, VectorXZ.Z_UNIT,
				VectorXZ.X_UNIT.invert(), VectorXZ.Z_UNIT.invert());

		//TODO (test): more + random vectors

	}

	private static final boolean almostEquals(double a, double b) {
		return abs(a - b) <= 0.001;
	}

	private static final boolean almostEquals(VectorXZ a, VectorXZ b) {
		return a.distanceTo(b) <= 0.001;
	}

	private static final boolean almostEquals(VectorXYZ a, VectorXYZ b) {
		return a.distanceTo(b) <= 0.001;
	}

	public static final void assertAlmostEquals(double expected, double actual) {
		if (!almostEquals(expected, actual)) {
			fail("expected " + expected + ", was " + actual);
		}
	}

	public static final void assertAlmostEquals(VectorXZ expected, VectorXZ actual) {
		if (!almostEquals(expected, actual)) {
			fail("expected " + expected + ", was " + actual);
		}
	}

	public static void assertAlmostEquals(double expectedX, double expectedZ, VectorXZ actual) {
		VectorXZ expected = new VectorXZ(expectedX, expectedZ);
		if (!almostEquals(expected, actual)) {
			fail("expected " + expected + ", was " + actual);
		}
	}

	public static final void assertAlmostEquals(VectorXYZ expected, VectorXYZ actual) {
		if (!almostEquals(expected, actual)) {
			fail("expected " + expected + ", was " + actual);
		}
	}

	public static final void assertAlmostEquals(double expectedX, double expectedY, double expectedZ,
			VectorXYZ actual) {
		VectorXYZ expected = new VectorXYZ(expectedX, expectedY, expectedZ);
		if (!almostEquals(expected, actual)) {
			fail("expected " + expected + ", was " + actual);
		}
	}

	public static final void assertAlmostEquals(List<VectorXZ> expected, List<VectorXZ> actual) {

		assertSame(expected.size(), actual.size());

		for (int i = 0; i < expected.size(); i++) {
			assertAlmostEquals(expected.get(i), actual.get(i));
		}

	}

	public static final void assertAlmostEqualsXYZ(List<VectorXYZ> expected, List<VectorXYZ> actual) {

		assertSame(expected.size(), actual.size());

		for (int i = 0; i < expected.size(); i++) {
			assertAlmostEquals(expected.get(i), actual.get(i));
		}

	}

	/**
	 * @throws AssertionError unless the two sets contain the "same" vectors
	 * (by the standards of {@link #assertAlmostEquals(VectorXZ, VectorXZ)})
	 */
	public static final void assertAlmostEquals(Set<VectorXZ> expected, Set<VectorXZ> actual) {

		assertSame(expected.size(), actual.size());

		for (VectorXZ expectedV : expected) {
			if (!actual.stream().anyMatch(actualV -> almostEquals(expectedV, actualV))) {
				fail("expected vector " + expectedV + " missing from " + actual);
			}
		}

	}

	public static final void assertAlmostEquals(SimplePolygonShapeXZ expected, SimplePolygonShapeXZ actual) {
		assertAlmostEquals(expected.getVertexListNoDup(), actual.getVertexListNoDup());
	}

	public static final void assertAlmostEquals(PolygonXYZ expected, PolygonXYZ actual) {
		assertAlmostEqualsXYZ(expected.getVertices(), actual.getVertices());
	}

	/**
	 * checks whether two sequences contain the same vectors in the same order,
	 * but allows them to start at different vectors in that sequence.
	 * This allows cyclic sequences (e.g. area outlines) to be treated as equal
	 * regardless of the arbitrary choice of start vector.
	 * When comparing vectors, a small difference is permitted to account for
	 * floating point arithmetics.
	 *
	 * @param reversible  if true, the order expected sequence can be mirrored
	 * @param actual  the actual sequence, to be compared with expected, != null
	 * @param expected  the expected sequence, != null
	 */
	public static final void assertSameCyclicOrder(boolean reversible,
			List<VectorXZ> actual, VectorXZ... expected) {

		if (actual.size() != expected.length) {
			fail("expected size " + expected.length + ", found list of size " + actual.size());
		}

		List<VectorXZ> actualModified = new ArrayList<>(actual);

		for (boolean reverse : asList(false, true)) {

			if (reverse) {

				if (!reversible) break;

				Collections.reverse(actualModified);

			}

			for (int offset = 0; offset < actualModified.size(); offset++) {

				boolean matches = true;

				for (int i = 0; i < actualModified.size(); i++) {
					int iWithOffset = (i + offset) % actualModified.size();
					if (VectorXZ.distance(expected[i], actualModified.get(iWithOffset)) > 0.0001) {
						matches = false;
						break;
					}
				}

				if (matches) {
					return;
				}

			}

		}

		fail("cannot match list to expected sequence. Found " + actualModified);

	}

	public static final void assertSameCyclicOrder(boolean reversible, List<VectorXZ> expected, List<VectorXZ> actual) {
		assertSameCyclicOrder(reversible, actual, expected.toArray(new VectorXZ[0]));
	}

}