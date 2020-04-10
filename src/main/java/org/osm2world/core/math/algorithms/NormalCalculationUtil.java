package org.osm2world.core.math.algorithms;

import static java.util.Arrays.asList;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.math.VectorXYZ.NULL_VECTOR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXYZWithNormals;
import org.osm2world.core.math.VectorXYZ;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public final class NormalCalculationUtil {

	/** prevents instantiation */
	private NormalCalculationUtil() {}

	/**
	 * calculates normals for the vertices of a list of triangles
	 */
	public static final List<VectorXYZ> calculateTriangleNormals(
			List<? extends TriangleXYZ> triangles, boolean smooth) {

		List<VectorXYZ> result = new ArrayList<>(triangles.size() * 3);

		if (!smooth) { //flat

			for (int triangle = 0; triangle < triangles.size(); triangle++) {
				VectorXYZ normal = triangles.get(triangle).getNormal();
				result.add(normal);
				result.add(normal);
				result.add(normal);
			}

		} else {

			Multimap<VectorXYZ, VectorXYZ> adjacentNormals = calculateAdjacentNormals(triangles);

			for (TriangleXYZ t : triangles) {
				for (VectorXYZ v : t.getVertices()) {
					result.add(averageNormal(adjacentNormals.get(v)));
				}
			}

		}

		return result;

	}


	public static final List<VectorXYZ> calculateTriangleStripNormals(
			List<VectorXYZ> vertices, boolean smooth) {

		assert vertices.size() >= 3;

		if (!smooth) {

			VectorXYZ[] normals = calculatePerTriangleNormals(vertices, false);
			return asList(normals);

		} else {

			List<VectorXYZ> result = new ArrayList<>(vertices.size());

			List<TriangleXYZ> triangles = trianglesFromTriangleStrip(vertices);
			Multimap<VectorXYZ, VectorXYZ> adjacentNormals = calculateAdjacentNormals(triangles);

			for (VectorXYZ v : vertices) {
				result.add(averageNormal(adjacentNormals.get(v)));
			}

			return result;

		}

	}

	public static final List<VectorXYZ> calculateTriangleFanNormals(
			List<VectorXYZ> vertices, boolean smooth) {

		assert vertices.size() >= 3;

		if (!smooth) {

			VectorXYZ[] normals = calculatePerTriangleNormals(vertices, true);
			return asList(normals);

		} else {

			List<VectorXYZ> result = new ArrayList<>(vertices.size());

			List<TriangleXYZ> triangles = trianglesFromTriangleFan(vertices);
			Multimap<VectorXYZ, VectorXYZ> adjacentNormals = calculateAdjacentNormals(triangles);

			for (VectorXYZ v : vertices) {
				result.add(averageNormal(adjacentNormals.get(v)));
			}

			return result;

		}

	}

	/**
	 * calculates "flat" lighting normals for triangle strips and triangle fans
	 *
	 * @param vertices  fan/strip vertices
	 * @param fan       true for fans, false for strips
	 */
	private static VectorXYZ[] calculatePerTriangleNormals(
			List<VectorXYZ> vertices, boolean fan) {

		VectorXYZ[] normals = new VectorXYZ[vertices.size()];

		for (int triangle = 0; triangle < vertices.size() - 2; triangle++) {

			int i = triangle + 1;

			VectorXYZ vBefore = vertices.get( fan ? 0 : (i-1) );
			VectorXYZ vAt = vertices.get(i);
			VectorXYZ vAfter = vertices.get(i+1);

			VectorXYZ toBefore = vBefore.subtract(vAt);
			VectorXYZ toAfter = vAfter.subtract(vAt);

			if (triangle % 2 == 0 || fan) {
				normals[i+1] = toBefore.crossNormalized(toAfter);
			} else {
				normals[i+1] = toAfter.crossNormalized(toBefore);
			}

		}

		normals[0] = normals[2];
		normals[1] = normals[2];

		return normals;

	}

	private static final double MAX_ANGLE_RADIANS = Math.toRadians(75);

	/**
	 * calculates normals for vertices that are shared by multiple triangles.
	 */
	public static final Collection<TriangleXYZWithNormals> calculateTrianglesWithNormals(
			Collection<TriangleXYZ> triangles) {

		Multimap<VectorXYZ, VectorXYZ> adjacentNormals = calculateAdjacentNormals(triangles);

		Collection<TriangleXYZWithNormals> result =
			new ArrayList<TriangleXYZWithNormals>(triangles.size());

		for (TriangleXYZ triangle : triangles) {

			result.add(new TriangleXYZWithNormals(triangle,
					calculateNormal(triangle.v1, triangle, adjacentNormals),
					calculateNormal(triangle.v2, triangle, adjacentNormals),
					calculateNormal(triangle.v3, triangle, adjacentNormals)));

		}

		return result;

	}

	private static VectorXYZ calculateNormal(VectorXYZ v, TriangleXYZ triangle,
			Multimap<VectorXYZ, VectorXYZ> adjacentNormals) {

		/* find adjacent triangles whose normals are close enough to that of t
		 * and save their normal vectors */

		List<VectorXYZ> relevantNormals = new ArrayList<VectorXYZ>();

		for (VectorXYZ normal : adjacentNormals.get(v)) {

			if (triangle.getNormal().angleTo(normal) <= MAX_ANGLE_RADIANS) {

				//add, unless one of the existing normals is very similar

				boolean notCoplanar = true;
				for (VectorXYZ n : relevantNormals) {
					if (n.angleTo(normal) < 0.01) {
						notCoplanar = false;
						break;
					}
				}

				if (notCoplanar) {
					relevantNormals.add(normal);
				}

			}
		}

		/* calculate sum of relevant normals,
		 * normalize it and set the result as normal for the vertex */

		return averageNormal(relevantNormals);

	}

	/** maps each vertex to all normal vectors of triangles adjacent to it */
	private static Multimap<VectorXYZ, VectorXYZ> calculateAdjacentNormals(List<? extends TriangleXYZ> triangles) {

		Multimap<VectorXYZ, VectorXYZ> result = HashMultimap.create();

		for (TriangleXYZ triangle : triangles) {
			for (VectorXYZ vertex : triangle.getVertices()) {
				result.put(vertex, triangle.getNormal());
			}
		}

		return result;

	}

	/** returns the normalized average of a collection of normal vectors */
	private static VectorXYZ averageNormal(Collection<VectorXYZ> normals) {
		VectorXYZ averageNormal = normals.stream().reduce(NULL_VECTOR, VectorXYZ::add);
		return averageNormal.normalize();
	}

}
