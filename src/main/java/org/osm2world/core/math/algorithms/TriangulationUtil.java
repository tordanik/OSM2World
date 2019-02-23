package org.osm2world.core.math.algorithms;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.locationtech.jts.triangulate.ConstraintEnforcementException;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;

/**
 * triangulation utility class that picks a suitable implementation, such as
 * {@link Earcut4JTriangulationUtil} or {@link JTSTriangulationUtil}
 */
public class TriangulationUtil {

	/**
	 * triangulates a two-dimensional polygon with holes and unconnected points.
	 */
	public static final List<TriangleXZ> triangulate(
			SimplePolygonXZ outerPolygon,
			Collection<SimplePolygonXZ> holes,
			Collection<VectorXZ> points) {

		if (points.isEmpty()) {
			return triangulate(outerPolygon, holes);
		}

		/* use JTS if there are unconnected points */

		try {

			return JTSTriangulationUtil.triangulate(outerPolygon, holes,
					Collections.<LineSegmentXZ>emptyList(), points);

		} catch (ConstraintEnforcementException e2) {

			/* JTS triangulation failed, falling back to earcut */

			// TODO: log warning

			return Earcut4JTriangulationUtil.triangulate(outerPolygon, holes, points);

		}

	}

	/**
	 * triangulates a two-dimensional polygon with holes.
	 */
	public static final List<TriangleXZ> triangulate(
			SimplePolygonXZ outerPolygon,
			Collection<SimplePolygonXZ> holes) {

		return Earcut4JTriangulationUtil.triangulate(outerPolygon, holes);

	}

	/**
	 * @see #triangulate(SimplePolygonXZ, Collection)
	 */
	public static final List<TriangleXZ> triangulate(
			PolygonWithHolesXZ polygon,
			Collection<VectorXZ> points) {

		return triangulate(polygon.getOuter(), polygon.getHoles(), points);

	}

	/**
	 * @see #triangulate(SimplePolygonXZ, Collection)
	 */
	public static final List<TriangleXZ> triangulate(
			PolygonWithHolesXZ polygon) {

		return triangulate(polygon.getOuter(), polygon.getHoles());

	}

}