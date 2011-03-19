package org.osm2world.core.math.algorithms;

import static org.osm2world.core.math.JTSConversionUtil.GF;
import static org.osm2world.core.math.JTSConversionUtil.polygonXZToJTSPolygon;
import static org.osm2world.core.math.JTSConversionUtil.polygonsXZFromJTSGeometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXZ;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.ConformingDelaunayTriangulationBuilder;

/**
 * uses the JTS library for triangulation.
 * Creates a Conforming Delaunay Triangulation with Steiner points!
 */
final class JTSTriangulationUtil {

	private static final Geometry[] EMPTY_GEOM_ARRAY = new Geometry[0];

	private JTSTriangulationUtil() { }
	
	/**
	 * triangulates a two-dimensional polygon
	 * by creating a simple polygon first
	 * (integrating holes into the polygon outline),
	 * then using Ear Clipping on that simple polygon
	 * 
	 * @param simplify  if true, the polygon will be modified in a way
	 *                  that improves the robustness of the algorithm,
	 *                  but does not preserve the original shape entirely.
	 *                  This should be used if a first attempt to triangulate failed.
	 */
	public static final List<TriangleXZ> triangulate(
			SimplePolygonXZ polygon,
			Collection<SimplePolygonXZ> holes) {
			
		ConformingDelaunayTriangulationBuilder triangulationBuilder =
			new ConformingDelaunayTriangulationBuilder();
		
		List<Polygon> polys = new ArrayList<Polygon>(holes.size() + 1);
		polys.add(polygonXZToJTSPolygon(polygon));
		for (SimplePolygonXZ hole : holes) {
			polys.add(polygonXZToJTSPolygon(hole));
		}
		
		ArrayList<Point> points = new ArrayList<Point>();
		
		triangulationBuilder.setSites(
				new GeometryCollection(points.toArray(EMPTY_GEOM_ARRAY), GF));
		triangulationBuilder.setConstraints(
				new GeometryCollection(polys.toArray(EMPTY_GEOM_ARRAY), GF));
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
			
			if (!triangleInHole && 
					polygon.contains(triangleAsPolygon.getOuter().getCenter())) { //TODO: create single method for this query within PolygonWithHoles
				
				triangles.add(triangleAsPolygon.asTriangleXZ());
				
			}
			
		}
			
		return triangles;
				
	}
	
}
