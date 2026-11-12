package org.osm2world.math.algorithms;

import static java.util.Collections.emptyList;
import static org.osm2world.math.algorithms.JTSConversionUtil.*;
import static org.osm2world.math.shapes.SimplePolygonXZ.asSimplePolygon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.triangulate.ConformingDelaunayTriangulationBuilder;
import org.locationtech.jts.triangulate.ConstraintEnforcementException;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.*;

/**
 * Uses the JTS library for triangulation. This triangulation library has the benefit of respecting inner points.
 * (Creates a Conforming Delaunay Triangulation with Steiner points.)
 */
public final class JTSTriangulationUtil {

	private static final Geometry[] EMPTY_GEOM_ARRAY = new Geometry[0];

	private JTSTriangulationUtil() { }

	/**
	 * triangulates a polygon with holes using on conforming delaunay triangulation
	 */
	public static List<TriangleXZ> triangulate(SimplePolygonShapeXZ polygon,
			Collection<? extends SimplePolygonShapeXZ> holes) {

		List<VectorXZ> points = emptyList();
		List<LineSegmentXZ> segments = emptyList();
		return triangulate(polygon, holes, segments, points);

	}

	/**
	 * Variant of {@link #triangulate(SimplePolygonShapeXZ, Collection)}
	 * that accepts some unconnected points within the polygon area
	 * and will try to create triangle vertices at these points.
	 * It will also accept line segments as edges that must be integrated
	 * into the resulting triangulation.
	 */
	public static List<TriangleXZ> triangulate(
			SimplePolygonShapeXZ polygon,
			Collection<? extends SimplePolygonShapeXZ> holes,
			Collection<LineSegmentXZ> segments,
			Collection<VectorXZ> points) {

		ConformingDelaunayTriangulationBuilder triangulationBuilder =
			new ConformingDelaunayTriangulationBuilder();

		List<Geometry> constraints = new ArrayList<>(1 + holes.size() + segments.size());

		constraints.add(toJTS(polygon));

		for (SimplePolygonShapeXZ hole : holes) {
			constraints.add(toJTS(hole));
		}

		for (LineSegmentXZ segment : segments) {
			constraints.add(toJTSLineString(segment));
		}

		ArrayList<Point> jtsPoints = new ArrayList<>();
		for (VectorXZ p : points) {
			CoordinateSequence coordinateSequence =
				new CoordinateArraySequence(new Coordinate[] {
						toJTS(p)});
			jtsPoints.add(new Point(coordinateSequence, GF));
		}

		triangulationBuilder.setSites(
				new GeometryCollection(jtsPoints.toArray(EMPTY_GEOM_ARRAY), GF));
		triangulationBuilder.setConstraints(
				new GeometryCollection(constraints.toArray(EMPTY_GEOM_ARRAY), GF));
		triangulationBuilder.setTolerance(0.01);

		/* run triangulation */

		Geometry triangulationResult;

		try {
			triangulationResult = triangulationBuilder.getTriangles(GF);
		} catch (ConstraintEnforcementException e) {
			// this happens for some inputs, such as long and thin polygons
			return triangulateFallback(polygon, holes, segments, points);
		}

		/* interpret the resulting polygons as triangles,
		 * filter out those which are outside the polygon or in a hole */

		Collection<PolygonWithHolesXZ> trianglesAsPolygons = polygonsFromJTS(triangulationResult);

		List<TriangleXZ> triangles = new ArrayList<>();

		for (PolygonWithHolesXZ triangleAsPolygon : trianglesAsPolygons) {
			TriangleXZ triangle = triangleAsPolygon.asTriangleXZ();
			VectorXZ tCenter = triangle.getCenter();
			if (polygon.contains(tCenter)
				&& holes.stream().noneMatch(hole -> hole.contains(tCenter))) {
				triangles.add(triangle);
			}

		}

		return triangles;

	}

	/** fallback using {@link TinfourTriangulationUtil} for inputs where the conforming Delaunay triangulation fails */
	private static List<TriangleXZ> triangulateFallback(
			SimplePolygonShapeXZ polygon,
			Collection<? extends SimplePolygonShapeXZ> holes,
			Collection<LineSegmentXZ> segments,
			Collection<VectorXZ> points) {

		List<PolygonWithHolesXZ> faces;

		if (segments.isEmpty()) {
			faces = List.of(new PolygonWithHolesXZ(asSimplePolygon(polygon),
					holes.stream().map(SimplePolygonXZ::asSimplePolygon).toList()));
		} else {
			faces = List.copyOf(FaceDecompositionUtil.splitPolygonIntoFaces(polygon, holes, segments));
		}

		List<TriangleXZ> result = new ArrayList<>();

		for (PolygonWithHolesXZ face : faces) {
			result.addAll(TinfourTriangulationUtil.triangulateWithoutAddingVertices(
					face.getOuter(), face.getHoles(), points));
		}

		return result;

	}

	public static List<TriangleXZ> triangulate(PolygonShapeXZ polygon) {
		return triangulate(polygon.getOuter(), polygon.getHoles());
	}

	public static List<TriangleXZ> triangulate(PolygonShapeXZ polygon, Collection<VectorXZ> points) {
		return triangulate(polygon.getOuter(), polygon.getHoles(), List.of(), points);
	}

	/** triangulates multiple polygons with holes and unconnected points */
	public static List<TriangleXZ> triangulate(Collection<PolygonShapeXZ> polygons, Collection<VectorXZ> points) {

		return polygons.stream()
				.map(p -> triangulate(p, points.stream().filter(p::contains).toList()))
				.flatMap(List::stream)
				.toList();

	}

}