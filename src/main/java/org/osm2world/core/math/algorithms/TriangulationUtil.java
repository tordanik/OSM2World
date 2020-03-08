package org.osm2world.core.math.algorithms;

import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;
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

}