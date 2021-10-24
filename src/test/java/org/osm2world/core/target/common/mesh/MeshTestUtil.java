package org.osm2world.core.target.common.mesh;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;

final class MeshTestUtil {

	private MeshTestUtil() {}

	/**
	 * asserts that the collection contains two triangles which together form the quad.
	 *
	 * @throws AssertionError  if the condition is not fulfilled
	 */
	static final void assertContainsQuad(List<TriangleXYZ> collection,
			VectorXYZ a, VectorXYZ b, VectorXYZ c, VectorXYZ d) {

		assertTrue(containsTriangle(collection, a, b, c) && containsTriangle(collection, a, c, d)
				|| containsTriangle(collection, a, b, d) && containsTriangle(collection, b, c, d));

	}

	/**
	 * returns true iff the collection contains the triangle defined by the vertices.
	 * The winding is checked, but otherwise the order of vertices does not matter.
	 */
	static final boolean containsTriangle(List<TriangleXYZ> collection,
			VectorXYZ v1, VectorXYZ v2, VectorXYZ v3) {

		for (TriangleXYZ t : collection) {

			if ((v1.equals(t.v1) && v2.equals(t.v2) && v3.equals(t.v3))
					|| v2.equals(t.v1) && v3.equals(t.v2) && v1.equals(t.v3)
					|| v3.equals(t.v1) && v1.equals(t.v2) && v2.equals(t.v3)) {
				return true;
			}

		}

		return false;

	}

}
