package org.osm2world.core.math.algorithms;

import static java.util.Collections.emptyList;
import static org.osm2world.core.math.JTSConversionUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.triangulate.ConformingDelaunayTriangulationBuilder;

/**
 * uses the JTS library for triangulation.
 * Creates a Conforming Delaunay Triangulation with Steiner points!
 */
public final class JTSTriangulationUtil {

	private static final Geometry[] EMPTY_GEOM_ARRAY = new Geometry[0];

	private JTSTriangulationUtil() { }
	
	/**
	 * triangulation of a polygon with holes, based on a
	 * conforming delaunay triangulation
	 */
	public static final List<TriangleXZ> triangulate(
			SimplePolygonXZ polygon,
			Collection<SimplePolygonXZ> holes) {
		
		List<VectorXZ> points = emptyList();
		List<LineSegmentXZ> segments = emptyList();
		return triangulate(polygon, holes, segments, points);
		
	}
	
	/**
	 * variant of {@link #triangulate(SimplePolygonXZ, Collection)}
	 * that accepts some unconnected points within the polygon area
	 * and will try to create triangle vertices at these points.
	 * It will also accept line segment as edges that must be integrated
	 * into the resulting triangulation.
	 */
	public static final List<TriangleXZ> triangulate(
			SimplePolygonXZ polygon,
			Collection<SimplePolygonXZ> holes,
			Collection<LineSegmentXZ> segments,
			Collection<VectorXZ> points) {
			
		ConformingDelaunayTriangulationBuilder triangulationBuilder =
			new ConformingDelaunayTriangulationBuilder();
		
		List<Geometry> constraints =
			new ArrayList<Geometry>(1 + holes.size() + segments.size());
		
		constraints.add(polygonXZToJTSPolygon(polygon));
		
		for (SimplePolygonXZ hole : holes) {
			constraints.add(polygonXZToJTSPolygon(hole));
		}
		
		for (LineSegmentXZ segment : segments) {
			constraints.add(lineSegmentXZToJTSLineString(segment));
		}
		
		ArrayList<Point> jtsPoints = new ArrayList<Point>();
		for (VectorXZ p : points) {
			CoordinateSequence coordinateSequence =
				new CoordinateArraySequence(new Coordinate[] {
						vectorXZToJTSCoordinate(p)});
			jtsPoints.add(new Point(coordinateSequence, GF));
		}
		
		triangulationBuilder.setSites(
				new GeometryCollection(jtsPoints.toArray(EMPTY_GEOM_ARRAY), GF));
		triangulationBuilder.setConstraints(
				new GeometryCollection(constraints.toArray(EMPTY_GEOM_ARRAY), GF));
		triangulationBuilder.setTolerance(0.01);
		
		/* run triangulation */
		
		Geometry triangulationResult = triangulationBuilder.getTriangles(GF);
		
		/* interpret the resulting polygons as triangles,
		 * filter out those which are outside the polygon or in a hole */
		
		Collection<PolygonWithHolesXZ> trianglesAsPolygons =
			polygonsXZFromJTSGeometry(triangulationResult);
		
		List<TriangleXZ> triangles = new ArrayList<TriangleXZ>();
		
		for (PolygonWithHolesXZ triangleAsPolygon : trianglesAsPolygons) {
			
			boolean triangleInHole = false;
			for (SimplePolygonXZ hole : holes) {
				if (hole.contains(triangleAsPolygon.getOuter().getCenter())) {
					triangleInHole = true;
					break;
				}
			}
			
			if (!triangleInHole && polygon.contains(
					triangleAsPolygon.getOuter().getCenter())) { //TODO: create single method for this query within PolygonWithHoles
				
				triangles.add(triangleAsPolygon.asTriangleXZ());
				
			}
			
		}
			
		return triangles;
				
	}
	
}