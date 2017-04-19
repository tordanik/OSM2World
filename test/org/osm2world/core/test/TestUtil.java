package org.osm2world.core.test;

import static java.util.Arrays.asList;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.plugins.graphview.core.data.EmptyTagGroup;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.osm.data.OSMNode;

public final class TestUtil {
	
	private TestUtil() {}
	
	/**
	 * returns a list of nodes where yon don't care about the attributes
	 */
	public static final List<OSMNode> createTestNodes(int numberOfNodes) {
		List<OSMNode> result = new ArrayList<OSMNode>(numberOfNodes);
		for (int i = 0; i < numberOfNodes; i++) {
			result.add(new OSMNode(i, i, EmptyTagGroup.EMPTY_TAG_GROUP, i));
		}
		return result;
	}
	
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
	
	public static final void assertAlmostEquals(
			double expected, double actual) {
		if (Math.abs(expected - actual) > 0.001) {
			throw new AssertionError("expected " + expected + ", was " + actual);
		}
	}
	
	public static final void assertAlmostEquals(
			VectorXZ expected, VectorXZ actual) {
		assertAlmostEquals(expected.x, actual.x);
		assertAlmostEquals(expected.z, actual.z);
	}
	
	public static void assertAlmostEquals(
			double expectedX, double expectedZ,
			VectorXZ actual) {
		assertAlmostEquals(expectedX, actual.x);
		assertAlmostEquals(expectedZ, actual.z);
	}
		
	public static final void assertAlmostEquals(
			VectorXYZ expected, VectorXYZ actual) {
		assertAlmostEquals(expected.x, actual.x);
		assertAlmostEquals(expected.y, actual.y);
		assertAlmostEquals(expected.z, actual.z);
	}
	
	public static final void assertAlmostEquals(
			double expectedX, double expectedY, double expectedZ,
			VectorXYZ actual) {
		assertAlmostEquals(expectedX, actual.x);
		assertAlmostEquals(expectedY, actual.y);
		assertAlmostEquals(expectedZ, actual.z);
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
			fail("expected size " + expected.length +
					", found list of size " + actual.size());
		}
		
		List<VectorXZ> actualModified = new ArrayList<VectorXZ>(actual);
		
		for (boolean reverse : asList(false, true)) {
			
			if (reverse) {
				
				if (!reversible) break;
				
				Collections.reverse(actualModified);
				
			}
			
			for (int offset = 0; offset < actualModified.size(); offset++) {
				
				boolean matches = true;
				
				for (int i = 0; i < actualModified.size(); i++) {
					int iWithOffset = (i + offset) % actualModified.size();
					if (VectorXZ.distance(expected[i],
							actualModified.get(iWithOffset)) > 0.0001) {
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
	
}