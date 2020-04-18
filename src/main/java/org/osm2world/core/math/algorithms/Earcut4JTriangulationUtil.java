package org.osm2world.core.math.algorithms;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.SimplePolygonShapeXZ;

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
			SimplePolygonShapeXZ polygon,
			Collection<? extends SimplePolygonShapeXZ> holes) {

		return triangulate(polygon, holes, emptyList());

	}

	/**
	 * variant of {@link #triangulate(SimplePolygonShapeXZ, Collection)}
	 * that accepts some unconnected points within the polygon area
	 * and will try to create triangle vertices at these points.
	 */
	public static final List<TriangleXZ> triangulate(
			SimplePolygonShapeXZ polygon,
			Collection<? extends SimplePolygonShapeXZ> holes,
			Collection<VectorXZ> points) {

		/* convert input data to the required format */

		int numVertices = polygon.size() + holes.stream().mapToInt(h -> h.size()).sum() + points.size() * 2;
		double[] data = new double[2 * numVertices];
		List<Integer> holeIndices = new ArrayList<>();

		int dataIndex = 0;

		for (VectorXZ v : polygon.getVertexListNoDup()) {
			data[2 * dataIndex] = v.x;
			data[2 * dataIndex + 1] = v.z;
			dataIndex ++;
		}

		for (SimplePolygonShapeXZ hole : holes) {

			holeIndices.add(dataIndex);

			for (VectorXZ v : hole.getVertexListNoDup()) {
				data[2 * dataIndex] = v.x;
				data[2 * dataIndex + 1] = v.z;
				dataIndex ++;
			}
		}

		/* points are simulated as holes with 2 almost identical points which are merged back together later.
		 * (Single-point holes get a special treatment by earcut4j and may not be contained in the result at all.) */

		Map<Integer, Integer> mergeIndexMap = new HashMap<>();

		for (VectorXZ point : points) {
			holeIndices.add(dataIndex);
			data[2 * dataIndex] = point.x;
			data[2 * dataIndex + 1] = point.z;
			data[2 * (dataIndex + 1)] = point.x + 1e-6;
			data[2 * (dataIndex + 1) + 1] = point.z + 1e-6;
			mergeIndexMap.put(dataIndex + 1, dataIndex);
			dataIndex +=2;
		}

		/* run the triangulation */

		List<Integer> rawResult = Earcut.earcut(data, Ints.toArray(holeIndices), 2);

		/* undo the duplication of individual points by merging their indices back together
		 * (this also requires checking triangles for duplicate indices later) */

		for (int i = 0; i < rawResult.size(); i++) {
			Integer newIndex = mergeIndexMap.get(rawResult.get(i));
			if (newIndex != null) {
				rawResult.set(i, newIndex);
			}
		}

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

				result.add(triangle);

			}

		}

		return result;

	}

	public static VectorXZ vectorAtIndex(double[] data, Integer index) {
		return new VectorXZ(data[2 * index], data[2 * index + 1]);
	}

}
