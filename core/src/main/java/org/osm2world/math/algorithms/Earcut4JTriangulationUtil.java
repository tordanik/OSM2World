package org.osm2world.math.algorithms;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.maplibre.earcut4j.Earcut;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.SimplePolygonShapeXZ;
import org.osm2world.math.shapes.TriangleXZ;

import com.google.common.primitives.Ints;

/**
 * Uses the earcut4j library for triangulation.
 */
public final class Earcut4JTriangulationUtil {

	/** prevents instantiation */
	private Earcut4JTriangulationUtil() {}

	/**
	 * triangulate a polygon with holes
	 */
	public static List<TriangleXZ> triangulate(
			SimplePolygonShapeXZ polygon,
			Collection<? extends SimplePolygonShapeXZ> holes) {

		return triangulate(polygon, holes, emptyList());

	}

	/**
	 * variant of {@link #triangulate(SimplePolygonShapeXZ, Collection)}
	 * that accepts some unconnected points within the polygon area
	 * and will try to create triangle vertices at these points.
	 */
	public static List<TriangleXZ> triangulate(
			SimplePolygonShapeXZ polygon,
			Collection<? extends SimplePolygonShapeXZ> holes,
			Collection<VectorXZ> points) {

		/* convert input data to the required format */

		int numVertices = polygon.size() + points.size() + holes.stream().mapToInt(SimplePolygonShapeXZ::size).sum();
		double[] data = new double[2 * numVertices];
		List<Integer> holeIndices = new ArrayList<>();

		int dataIndex = 0;

		for (VectorXZ v : polygon.verticesNoDup()) {
			data[2 * dataIndex] = v.x;
			data[2 * dataIndex + 1] = v.z;
			dataIndex ++;
		}

		for (SimplePolygonShapeXZ hole : holes) {

			holeIndices.add(dataIndex);

			for (VectorXZ v : hole.verticesNoDup()) {
				data[2 * dataIndex] = v.x;
				data[2 * dataIndex + 1] = v.z;
				dataIndex ++;
			}
		}

		/* points are added as single-vertex holes */

		for (VectorXZ point : points) {
			holeIndices.add(dataIndex);
			data[2 * dataIndex] = point.x;
			data[2 * dataIndex + 1] = point.z;
			dataIndex +=1;
		}

		/* run the triangulation */

		List<Integer> rawResult = Earcut.earcut(data, Ints.toArray(holeIndices), 2);

		/* turn the result (index lists) into TriangleXZ instances */

		assert rawResult.size() % 3 == 0;

		List<TriangleXZ> result = new ArrayList<>(rawResult.size() / 3);

		for (int i = 0; i < rawResult.size() / 3; i++) {

			TriangleXZ triangle = new TriangleXZ(
					vectorAtIndex(data, rawResult.get(3*i)),
					vectorAtIndex(data, rawResult.get(3*i + 1)),
					vectorAtIndex(data, rawResult.get(3*i + 2)));

			if (!triangle.v1.equals(triangle.v2)
					&& !triangle.v2.equals(triangle.v3)
					&& !triangle.v3.equals(triangle.v1)) {  // check required due to workaround for individual points

				if (!triangle.isDegenerateOrNaN()) {
					result.add(triangle);
				}

			}

		}

		return result;

	}

	public static VectorXZ vectorAtIndex(double[] data, Integer index) {
		return new VectorXZ(data[2 * index], data[2 * index + 1]);
	}

}
