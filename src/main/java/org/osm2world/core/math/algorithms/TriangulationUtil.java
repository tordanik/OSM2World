package org.osm2world.core.math.algorithms;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osm2world.core.math.*;
import org.osm2world.core.math.shapes.SimplePolygonShapeXZ;

/**
 * triangulation utility class that picks a suitable implementation, such as
 * {@link Earcut4JTriangulationUtil} or {@link JTSTriangulationUtil}
 */
public class TriangulationUtil {

	/**
	 * triangulates a two-dimensional polygon with holes and unconnected points.
	 */
	public static final List<TriangleXZ> triangulate(
			SimplePolygonShapeXZ outerPolygon,
			Collection<? extends SimplePolygonShapeXZ> holes,
			Collection<VectorXZ> points) {

		return Earcut4JTriangulationUtil.triangulate(outerPolygon, holes, points);

	}

	/**
	 * triangulates a two-dimensional polygon with holes.
	 */
	public static final List<TriangleXZ> triangulate(
			SimplePolygonShapeXZ outerPolygon,
			Collection<? extends SimplePolygonShapeXZ> holes) {

		return Earcut4JTriangulationUtil.triangulate(outerPolygon, holes);

	}

	/**
	 * @see #triangulate(SimplePolygonShapeXZ, Collection)
	 */
	public static final List<TriangleXZ> triangulate(
			PolygonWithHolesXZ polygon,
			Collection<VectorXZ> points) {

		return triangulate(polygon.getOuter(), polygon.getHoles(), points);

	}

	/**
	 * @see #triangulate(SimplePolygonShapeXZ, Collection)
	 */
	public static final List<TriangleXZ> triangulate(
			PolygonWithHolesXZ polygon) {

		return triangulate(polygon.getOuter(), polygon.getHoles());

	}

	/**
	 * triangulates a 3D polygon.
	 * Only works if the polygon can be projected into a simple 2D polygon in the XZ plane.
	 */
	public static List<TriangleXYZ> triangulateXYZ(PolygonXYZ polygon) {

		SimplePolygonXZ xzPolygon = polygon.getSimpleXZPolygon();

		List<TriangleXZ> resultXZ = triangulate(xzPolygon, List.of());

		Map<VectorXZ, VectorXYZ> eleMap = new HashMap<>();
		polygon.vertices().forEach(v -> eleMap.put(v.xz(), v));
		// TODO handle extra vertices (which were not present in the original polygon) with interpolation

		return triangulationXZtoXYZ(resultXZ, eleMap::get);

	}

	/**
	 * adds elevation (y coordinates) to an existing triangulation in the XZ plane
	 */
	public static List<TriangleXYZ> triangulationXZtoXYZ(List<? extends TriangleXZ> trianglesXZ,
														 Function<VectorXZ, VectorXYZ> xyzFunction) {
		//TODO: ccw test should not be in here, but maybe in 2D part of TriangulationUtil
		return trianglesXZ.stream()
				.map(t -> t.makeCounterclockwise().xyz(xyzFunction))
				.toList();
	}

}