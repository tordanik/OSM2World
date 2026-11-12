package org.osm2world.math.algorithms;

import static org.osm2world.math.algorithms.JTSConversionUtil.toJTS;

import java.util.*;

import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.operation.distance.IndexedFacetDistance;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.*;
import org.tinfour.common.*;
import org.tinfour.standard.IncrementalTin;

/**
 * Uses the Tinfour library for constrained Delaunay triangulation.
 */
public final class TinfourTriangulationUtil {

	/** prevents instantiation */
	private TinfourTriangulationUtil() {}

	/** points closer than this to the polygon's outline are ignored. */
	private static final double MIN_POINT_DISTANCE_TO_OUTLINE = 1e-3;

	/**
	 * the nominal point spacing passed to Tinfour, which determines its tolerances.
	 * This is much smaller than the typical distance between vertices because Tinfour cannot even start
	 * triangulating thin polygons (such as slivers along the edges of other objects) with larger values.
	 */
	private static final double NOMINAL_POINT_SPACING = 0.01;

	/**
	 * Tinfour implementation of {@link TriangulationUtil#triangulateWithoutAddingVertices(PolygonShapeXZ, Collection)}.
	 * Creates a constrained Delaunay triangulation of a polygon with holes and unconnected points within it.
	 *
	 * @throws IllegalStateException  if Tinfour could not triangulate the polygon without adding vertices,
	 *                                e.g. because some of the polygon's vertices are almost identical
	 */
	public static List<TriangleXZ> triangulateWithoutAddingVertices(
			SimplePolygonShapeXZ outerPolygon,
			Collection<? extends SimplePolygonShapeXZ> holes,
			Collection<VectorXZ> points) {

		var polygon = new PolygonWithHolesXZ(SimplePolygonXZ.asSimplePolygon(outerPolygon),
				holes.stream().map(SimplePolygonXZ::asSimplePolygon).toList());

		/* create Tinfour vertices, remembering the original coordinates */

		List<VectorXZ> pointsInside = pointsInside(polygon, points);

		if (pointsInside.isEmpty() && polygon.getHoles().isEmpty() && polygon.getOuter().size() == 3) {
			// nothing to triangulate, and Tinfour can't triangulate some tiny triangles
			List<VectorXZ> vs = polygon.getOuter().verticesNoDup();
			return List.of(new TriangleXZ(vs.get(0), vs.get(1), vs.get(2)));
		}

		Map<Vertex, VectorXZ> originalPositions = new IdentityHashMap<>();

		List<Vertex> pointVertices = new ArrayList<>();
		for (VectorXZ p : pointsInside) {
			pointVertices.add(createVertex(p, originalPositions));
		}

		List<IConstraint> constraints = new ArrayList<>();
		constraints.add(createConstraint(polygon.getOuter().makeCounterclockwise(), originalPositions));
		for (SimplePolygonXZ hole : polygon.getHoles()) {
			constraints.add(createConstraint(hole.makeClockwise(), originalPositions));
		}

		/* triangulate */

		var tin = new IncrementalTin(NOMINAL_POINT_SPACING);
		tin.add(pointVertices, null);
		tin.addConstraints(constraints, false);

		if (tin.getSyntheticVertexCount() > 0) {
			throw new IllegalStateException("Tinfour added vertices to the triangulation");
		}

		/* convert the triangles inside the polygon */

		List<TriangleXZ> result = new ArrayList<>();

		for (SimpleTriangle t : tin.triangles()) {
			if (t.isGhost()) continue;
			var triangle = new TriangleXZ(
					originalPosition(t.getVertexA(), originalPositions),
					originalPosition(t.getVertexB(), originalPositions),
					originalPosition(t.getVertexC(), originalPositions));
			if (polygon.contains(triangle.getCenter())) {
				result.add(triangle);
			}
		}

		return result;

	}

	/** returns the points which are inside the polygon and not too close to its outline */
	private static List<VectorXZ> pointsInside(PolygonWithHolesXZ polygon, Collection<VectorXZ> points) {

		if (points.isEmpty()) return List.of();

		Geometry polygonJTS = toJTS(polygon);
		var locator = new IndexedPointInAreaLocator(polygonJTS);
		var outlineDistance = new IndexedFacetDistance(polygonJTS.getBoundary());

		List<VectorXZ> result = new ArrayList<>();

		for (VectorXZ p : points) {
			if (locator.locate(toJTS(p)) == Location.INTERIOR
					&& outlineDistance.distance(JTSConversionUtil.GF.createPoint(toJTS(p)))
							>= MIN_POINT_DISTANCE_TO_OUTLINE) {
				result.add(p);
			}
		}

		return result;

	}

	/**
	 * @param ring  a ring of the polygon, ccw for the outer ring and clockwise for holes (Tinfour standard)
	 */
	private static PolygonConstraint createConstraint(SimplePolygonXZ ring, Map<Vertex, VectorXZ> originalPositions) {
		List<Vertex> vertices = new ArrayList<>();
		for (VectorXZ v : ring.verticesNoDup()) {
			vertices.add(createVertex(v, originalPositions));
		}
		var constraint = new PolygonConstraint(vertices);
		constraint.complete();
		return constraint;
	}

	private static Vertex createVertex(VectorXZ v, Map<Vertex, VectorXZ> originalPositions) {
		var vertex = new Vertex(v.x, v.z, 0, originalPositions.size());
		originalPositions.put(vertex, v);
		return vertex;
	}

	/**
	 * returns the original position of a vertex of the triangulation,
	 * which avoids any changes to the coordinates within Tinfour
	 */
	private static VectorXZ originalPosition(Vertex vertex, Map<Vertex, VectorXZ> originalPositions) {
		VectorXZ result = originalPositions.get(vertex);
		if (result == null && vertex instanceof VertexMergerGroup group) {
			// (almost) identical input vertices which Tinfour has merged
			for (Vertex v : group.getVertices()) {
				result = originalPositions.get(v);
				if (result != null) break;
			}
		}
		if (result == null) {
			throw new IllegalStateException("unexpected vertex in the triangulation: " + vertex);
		}
		return result;
	}

}
