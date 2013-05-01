package org.osm2world.core.math.algorithms;

import static org.osm2world.core.math.JTSConversionUtil.polygonXZToJTSPolygon;
import static org.osm2world.core.math.JTSConversionUtil.polygonsXZFromJTSGeometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Polygon;

/**
 * utility class for Constructive Area Geometry (CAG),
 * boolean operations on areas
 */
public final class CAGUtil {
	
	private CAGUtil() { }
	
	/**
	 * takes a polygon outline, "subtracts" a collection of other polygon outlines, 
	 * and returns a collection of polygons that covers the difference area.
	 * 
	 * The result polygons should cover the area that was
	 * within the original polygon (excluding its holes),
	 * but not within a subtracted polygon.
	 * 
	 * @return
	 * 	 polygons without self-intersections, but maybe with holes
	 */
	public static final Collection<PolygonWithHolesXZ> subtractPolygons(
			SimplePolygonXZ basePolygon,
			List<? extends SimplePolygonXZ> subtractPolygons) {
		
		List<Geometry> remainingGeometry = Collections.singletonList(
				(Geometry)polygonXZToJTSPolygon(basePolygon));
		
		for (SimplePolygonXZ subtractPolygon : subtractPolygons) {
			
			List<Geometry> newRemainingGeometry = new ArrayList<Geometry>(1);
			
			for (Geometry g : remainingGeometry) {
				
				Geometry newG = g.difference(
						polygonXZToJTSPolygon(subtractPolygon));
				
				if (newG instanceof GeometryCollection) {
					for (int i = 0; i < ((GeometryCollection)newG).getNumGeometries(); i++) {
						newRemainingGeometry.add(((GeometryCollection)newG).getGeometryN(i));
					}
				} else {
					newRemainingGeometry.add(newG);
				}			
				
			}
			
			remainingGeometry = newRemainingGeometry;
						
		}
				
		Collection<PolygonWithHolesXZ> result =
			new ArrayList<PolygonWithHolesXZ>();
		
		for (Geometry g : remainingGeometry) {
			result.addAll(polygonsXZFromJTSGeometry(g));
		}
		
		return result;
		
	}

	/**
	 * calculates the intersection area of a collection of polygons.
	 * 
	 * The result polygons should cover the area that was
	 * within all of the polygons.
	 */
	public static final Collection<PolygonWithHolesXZ> intersectPolygons(
			List<? extends SimplePolygonXZ> intersectPolygons) {
		
		if (intersectPolygons.isEmpty()) { throw new IllegalArgumentException(); }
				
		Geometry remainingGeometry = null;
		
		for (SimplePolygonXZ poly : intersectPolygons) {
			
			Polygon jtsPoly = polygonXZToJTSPolygon(poly);
			
			if (remainingGeometry == null) {
				remainingGeometry = jtsPoly;
			} else {
				remainingGeometry = remainingGeometry.intersection(jtsPoly);
			}
			
		}
		
		return polygonsXZFromJTSGeometry(remainingGeometry);
				
	}
	
}
