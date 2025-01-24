package org.osm2world.core.math.algorithms;

import static java.util.Collections.disjoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.util.exception.TriangulationException;
import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.Polygon;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;
import org.poly2tri.triangulation.point.TPoint;

/**
 * uses the poly2tri library for triangulation.
 * Creates a Constrained Delaunay Triangulation, not true Delaunay!
 */
public final class Poly2TriTriangulationUtil {

	private Poly2TriTriangulationUtil() { }

	/**
	 * triangulates of a polygon with holes.
	 *
	 * Accepts some unconnected points within the polygon area
	 * and will create triangle vertices at these points.
	 * It will also accept line segments as edges that must be integrated
	 * into the resulting triangulation.
	 * @throws TriangulationException if triangulation fails
	 */
	public static final List<TriangleXZ> triangulate(
			SimplePolygonXZ outerPolygon,
			Collection<SimplePolygonXZ> holes,
			Collection<LineSegmentXZ> segments,
			Collection<VectorXZ> points) throws TriangulationException {

		/* remove any problematic data (duplicate points) from the input */

		Set<VectorXZ> knownVectors =
				new HashSet<VectorXZ>(outerPolygon.getVertexCollection());

		List<SimplePolygonXZ> filteredHoles = new ArrayList<SimplePolygonXZ>();

		for (SimplePolygonXZ hole : holes) {

			if (disjoint(hole.getVertexCollection(), knownVectors)) {
				filteredHoles.add(hole);
				knownVectors.addAll(hole.getVertices());
			}

		}

		//TODO filter segments

		Set<VectorXZ> filteredPoints = new HashSet<VectorXZ>(points);
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
	 * variant of {@link #triangulate(SimplePolygonXZ, Collection, Collection, Collection)}
	 * that does not validate the input. This is obviously faster,
	 * but the caller needs to make sure that there are no problems.
	 * @throws TriangulationException if triangulation fails
	 */
	public static final List<TriangleXZ> triangulateFast(
			SimplePolygonXZ outerPolygon,
			Collection<SimplePolygonXZ> holes,
			Collection<LineSegmentXZ> segments,
			Collection<VectorXZ> points) throws TriangulationException {

		/* prepare data for triangulation */

		Polygon triangulationPolygon = toPolygon(outerPolygon);

		for (SimplePolygonXZ hole : holes) {
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

			List<TriangleXZ> result = new ArrayList<TriangleXZ>(triangles.size());

			for (DelaunayTriangle triangle : triangles) {
				result.add(toTriangleXZ(triangle));
			}

			return result;

		} catch (Exception e) {
			throw new TriangulationException(e);
		} catch (StackOverflowError e) {
			throw new TriangulationException(e);
		}

	}

	private static final TPoint toTPoint(VectorXZ v) {
		return new TPoint(v.x, v.z);
	}

	private static final VectorXZ toVectorXZ(TriangulationPoint points) {
		return new VectorXZ(points.getX(), points.getY());
	}

	private static final Polygon toPolygon(SimplePolygonXZ polygon) {

		List<PolygonPoint> points = new ArrayList<PolygonPoint>(polygon.size());

		for (VectorXZ v : polygon.getVertices()) {
			points.add(new PolygonPoint(v.x, v.z));
		}

		return new Polygon(points);

	}

	private static final TriangleXZ toTriangleXZ(DelaunayTriangle triangle) {

		return new TriangleXZ(
				toVectorXZ(triangle.points[0]),
				toVectorXZ(triangle.points[1]),
				toVectorXZ(triangle.points[2]));

	}

}