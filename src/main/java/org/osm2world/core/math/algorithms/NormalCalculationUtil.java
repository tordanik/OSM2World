package org.osm2world.core.math.algorithms;

import static java.util.Arrays.asList;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.math.VectorXYZ.NULL_VECTOR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.TriangleXYZ;
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
				for (VectorXYZ v : t.verticesNoDup()) {
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

	/** maps each vertex to all normal vectors of triangles adjacent to it */
	private static Multimap<VectorXYZ, VectorXYZ> calculateAdjacentNormals(List<? extends TriangleXYZ> triangles) {

		Multimap<VectorXYZ, VectorXYZ> result = HashMultimap.create();

		for (TriangleXYZ triangle : triangles) {
			for (VectorXYZ vertex : triangle.verticesNoDup()) {
				result.put(vertex, triangle.getNormal());
			}
		}

		return result;

	}

	/** returns the normalized average of a collection of normal vectors */
	private static VectorXYZ averageNormal(Collection<VectorXYZ> normals) {
		VectorXYZ averageNormal = normals.stream().reduce(NULL_VECTOR, VectorXYZ::add);
		return (averageNormal.lengthSquared() > 0) ? averageNormal.normalize() : normals.iterator().next();
	}

}
