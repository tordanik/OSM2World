package org.osm2world.math.algorithms;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.*;

/**
 * triangulation utility class that picks a suitable implementation, such as
 * {@link Earcut4JTriangulationUtil} or {@link JTSTriangulationUtil}
 */
public final class TriangulationUtil {

	/** prevents instantiation */
	private TriangulationUtil() {}

	/**
	 * triangulates a two-dimensional polygon with holes and unconnected points.
	 */
	public static List<TriangleXZ> triangulate(
			SimplePolygonShapeXZ outerPolygon,
			Collection<? extends SimplePolygonShapeXZ> holes,
			Collection<VectorXZ> points) {

		return Earcut4JTriangulationUtil.triangulate(outerPolygon, holes, points);

	}

	/**
	 * triangulates a two-dimensional polygon with holes.
	 */
	public static List<TriangleXZ> triangulate(
			SimplePolygonShapeXZ outerPolygon,
			Collection<? extends SimplePolygonShapeXZ> holes) {

		return Earcut4JTriangulationUtil.triangulate(outerPolygon, holes);

	}

	/** triangulates multiple polygons with holes and unconnected points */
	public static List<TriangleXZ> triangulate(
			Collection<PolygonShapeXZ> polygons,
			Collection<VectorXZ> points) {

		return polygons.stream()
				.map(p -> triangulate(p.getOuter(), p.getHoles(), points.stream().filter(p::contains).toList()))
				.flatMap(List::stream)
				.toList();

	}

	/** variant of {@link #triangulate(Collection, Collection)} with no unconnected points */
	public static List<TriangleXZ> triangulate(
			Collection<PolygonShapeXZ> polygons) {
		return triangulate(polygons, List.of());
	}

	/**
	 * @see #triangulate(SimplePolygonShapeXZ, Collection)
	 */
	public static List<TriangleXZ> triangulate(
			PolygonShapeXZ polygon) {

		return triangulate(polygon.getOuter(), polygon.getHoles());

	}

	/**
	 * triangulates a polygon with holes and unconnected points within it, without adding any other vertices.
	 * In particular, no vertices are added to the polygon's outline (to prevent cracks between adjacent,
	 * separately triangulated polygons).
	 * Attempts to avoid narrow triangles as far as possible.
	 * Points which are not inside the polygon, or very close to its outline, are ignored.
	 */
	public static List<TriangleXZ> triangulateWithoutAddingVertices(PolygonShapeXZ polygon,
			Collection<VectorXZ> points) {

		List<TriangleXZ> result;

		try {
			result = TinfourTriangulationUtil.triangulateWithoutAddingVertices(
					polygon.getOuter(), polygon.getHoles(), points);
		} catch (RuntimeException e) {
			ConversionLog.warn("Triangulation failed, ignoring the points within the polygon: " + e.getMessage(), e);
			result = triangulate(polygon.getOuter(), polygon.getHoles());
		}

		// remove very small triangles which would cause errors when constructing TriangleXYZs from them
		result = result.stream().filter(t -> t.getArea() >= 1e-5).toList();

		return result;

	}

	/**
	 * Triangulates a 3D polygon.
	 * Only works if the polygon can be projected into a simple 2D polygon in the XZ plane.
	 */
	public static List<TriangleXYZ> triangulateXYZ(PolygonXYZ polygon) {

		SimplePolygonXZ xzPolygon = polygon.getSimpleXZPolygon();

		List<TriangleXZ> resultXZ = triangulate(xzPolygon);

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