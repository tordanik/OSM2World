package org.osm2world.math.algorithms;

import static java.util.Collections.disjoint;

import java.util.*;

import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.SimplePolygonShapeXZ;
import org.osm2world.math.shapes.TriangleXZ;
import org.osm2world.util.exception.TriangulationException;
import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.Polygon;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;
import org.poly2tri.triangulation.point.TPoint;

/**
 * Uses the poly2tri library for triangulation.
 * Creates a Constrained Delaunay Triangulation, not true Delaunay!
 */
public final class Poly2TriTriangulationUtil {

	private Poly2TriTriangulationUtil() { }

	/**
	 * Triangulates a polygon with holes.
	 * Accepts some unconnected points within the polygon area
	 * and will create triangle vertices at these points.
	 * It will also accept line segments as edges that must be integrated
	 * into the resulting triangulation.
	 * @throws TriangulationException if triangulation fails
	 */
	public static List<TriangleXZ> triangulate(
			SimplePolygonShapeXZ outerPolygon,
			Collection<? extends SimplePolygonShapeXZ> holes,
			Collection<LineSegmentXZ> segments,
			Collection<VectorXZ> points) throws TriangulationException {

		/* remove any problematic data (duplicate points) from the input */

		Set<VectorXZ> knownVectors = new HashSet<>(outerPolygon.vertices());

		List<SimplePolygonShapeXZ> filteredHoles = new ArrayList<>();

		for (SimplePolygonShapeXZ hole : holes) {

			if (disjoint(hole.verticesNoDup(), knownVectors)) {
				filteredHoles.add(hole);
				knownVectors.addAll(hole.verticesNoDup());
			}

		}

		//TODO filter segments

		Set<VectorXZ> filteredPoints = new HashSet<>(points);
		filteredPoints.removeAll(knownVectors);

		// remove points that are *almost* the same as a known vector
		Iterator<VectorXZ> filteredPointsIterator = filteredPoints.iterator();
		while (filteredPointsIterator.hasNext()) {
			VectorXZ filteredPoint = filteredPointsIterator.next();
			for (VectorXZ knownVector : knownVectors) {
				if (knownVector.distanceTo(filteredPoint) < 0.2) {
					filteredPointsIterator.remove();
					break;
				}
			}
		}

		/* run the actual triangulation */

		return triangulateFast(outerPolygon, filteredHoles, segments, filteredPoints);

	}

	/**
	 * Variant of {@link #triangulate(SimplePolygonShapeXZ, Collection, Collection, Collection)}
	 * that does not validate the input. This is obviously faster,
	 * but the caller needs to make sure that there are no problems.
	 * @throws TriangulationException if triangulation fails
	 */
	public static List<TriangleXZ> triangulateFast(
			SimplePolygonShapeXZ outerPolygon,
			Collection<? extends SimplePolygonShapeXZ> holes,
			Collection<LineSegmentXZ> segments,
			Collection<VectorXZ> points) throws TriangulationException {

		/* prepare data for triangulation */

		Polygon triangulationPolygon = toPolygon(outerPolygon);

		for (SimplePolygonShapeXZ hole : holes) {
			triangulationPolygon.addHole(toPolygon(hole));
		}

		//TODO collect points and constraints from segments

		for (VectorXZ p : points) {
			triangulationPolygon.addSteinerPoint(toTPoint(p));
		}

		try {

			/* run triangulation */

			Poly2Tri.triangulate(triangulationPolygon);

			/* convert the result to the desired format */

			List<DelaunayTriangle> triangles = triangulationPolygon.getTriangles();

			List<TriangleXZ> result = new ArrayList<>(triangles.size());

			for (DelaunayTriangle triangle : triangles) {
				result.add(toTriangleXZ(triangle));
			}

			return result;

		} catch (Exception | StackOverflowError e) {
			throw new TriangulationException(e);
		}

	}

	private static TPoint toTPoint(VectorXZ v) {
		return new TPoint(v.x, v.z);
	}

	private static VectorXZ toVectorXZ(TriangulationPoint points) {
		return new VectorXZ(points.getX(), points.getY());
	}

	private static Polygon toPolygon(SimplePolygonShapeXZ polygon) {

		List<PolygonPoint> points = new ArrayList<>(polygon.size());

		for (VectorXZ v : polygon.verticesNoDup()) {
			points.add(new PolygonPoint(v.x, v.z));
		}

		return new Polygon(points);

	}

	private static TriangleXZ toTriangleXZ(DelaunayTriangle triangle) {

		return new TriangleXZ(
				toVectorXZ(triangle.points[0]),
				toVectorXZ(triangle.points[1]),
				toVectorXZ(triangle.points[2]));

	}

}