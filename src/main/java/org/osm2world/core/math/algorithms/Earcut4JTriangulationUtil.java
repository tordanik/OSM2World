package org.osm2world.core.math.algorithms;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;

import com.google.common.primitives.Ints;

import earcut4j.Earcut;

/**
 * uses the earcut4j library for triangulation.
 */
public class Earcut4JTriangulationUtil {

	/**
	 * triangulate a polygon with holes
	 */
	public static final List<TriangleXZ> triangulate(
			SimplePolygonXZ polygon,
			Collection<SimplePolygonXZ> holes) {

		return triangulate(polygon, holes, emptyList());

	}

	/**
	 * variant of {@link #triangulate(SimplePolygonXZ, Collection)}
	 * that accepts some unconnected points within the polygon area
	 * and will try to create triangle vertices at these points.
	 */
	public static final List<TriangleXZ> triangulate(
			SimplePolygonXZ polygon,
			Collection<SimplePolygonXZ> holes,
			Collection<VectorXZ> points) {

		/* convert input data to the required format */

		int numVertices = polygon.size() + holes.stream().mapToInt(h -> h.size()).sum();
		double[] data = new double[2 * numVertices];
		List<Integer> holeIndices = new ArrayList<>();

		int dataIndex = 0;

		for (VectorXZ v : polygon.getVertices()) {
			data[2 * dataIndex] = v.x;
			data[2 * dataIndex + 1] = v.z;
			dataIndex ++;
		}

		for (SimplePolygonXZ hole : holes) {

			holeIndices.add(dataIndex);

			for (VectorXZ v : hole.getVertices()) {
				data[2 * dataIndex] = v.x;
				data[2 * dataIndex + 1] = v.z;
				dataIndex ++;
			}
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

			boolean containsNoPoints = true;

			for (VectorXZ point : points) {
				if (triangle.contains(point)) {
					containsNoPoints = false;
					result.add(new TriangleXZ(point, triangle.v2, triangle.v3));
					result.add(new TriangleXZ(triangle.v1, point, triangle.v3));
					result.add(new TriangleXZ(triangle.v1, triangle.v2, point));
					break;
				}
			}

			if (containsNoPoints) {
				result.add(triangle);
			}

		}

		return result;

	}

	public static VectorXZ vectorAtIndex(double[] data, Integer index) {
		return new VectorXZ(data[2 * index], data[2 * index + 1]);
	}

}
