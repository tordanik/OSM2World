package org.osm2world.core.math.algorithms;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;

import com.vividsolutions.jts.triangulate.ConstraintEnforcementException;

/**
 * triangulation utility class that uses {@link EarClippingTriangulationUtil},
 * and, as a fallback, {@link JTSTriangulationUtil}
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
			
			try {
				
				return EarClippingTriangulationUtil.triangulate(outerPolygon, holes);
				
			} catch (InvalidGeometryException e) {
				
				//TODO (error handling): log failed triangulations properly (as info)
				
				// e.printStackTrace();
				// System.err.println("outer: " + outerPolygon);
				// System.err.println("holes: " + holes);
				// System.err.println("using JTS triangulation instead");
				
			}
			
		}
		
		/* use JTS if there are unconnected points, or as a fallback */
		
		try {

			return JTSTriangulationUtil.triangulate(outerPolygon, holes,
					Collections.<LineSegmentXZ>emptyList(), points);
			
		} catch (ConstraintEnforcementException e2) {
			
			e2.printStackTrace();
			System.err.println("outer: " + outerPolygon);
			System.err.println("holes: " + holes);
			System.err.println("JTS triangulation failed, returning empty list");
			
			return Collections.emptyList();
			
		}
		
	}
	
	/**
	 * triangulates a two-dimensional polygon with holes.
	 */
	public static final List<TriangleXZ> triangulate(
			SimplePolygonXZ outerPolygon,
			Collection<SimplePolygonXZ> holes) {
		
		return triangulate(outerPolygon, holes,
				Collections.<VectorXZ>emptyList());
		
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