package org.osm2world.core.math;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

/**
 * converts between own and JTS geometry representations.
 * 
 * When handling three-dimensional coordinates, the y and z information will
 * be swapped when converting from or to JTS. JTS uses x and y as 2D
 * plane, z as elevation info.
 */
public class JTSConversionUtil {

	public static final GeometryFactory GF = new GeometryFactory();
	
	public static final Coordinate vectorXZToJTSCoordinate(VectorXZ v) {
		return new Coordinate(v.x, v.z);
	}
	
	public static final Coordinate vectorXYZToJTSCoordinate(VectorXYZ v) {
		return new Coordinate(v.x, v.z, v.y);
	}

	public static final VectorXZ vectorXZFromJTSCoordinate(Coordinate c) {
		return new VectorXZ(c.x, c.y);
	}
	
	public static final VectorXYZ vectorXYZFromJTSCoordinate(Coordinate c) {
		return new VectorXYZ(c.x, c.z, c.y);
	}

	public static LineString lineSegmentXZToJTSLineString(LineSegmentXZ segment) {
		
		Coordinate[] points = {
				vectorXZToJTSCoordinate(segment.p1),
				vectorXZToJTSCoordinate(segment.p2)
		};
		
		return new LineString(new CoordinateArraySequence(points), GF);
		
	}
	
	public static final Polygon polygonXZToJTSPolygon(SimplePolygonXZ polygon) {
	
		List<VectorXZ> vertices = polygon.getVertexLoop();
		
		Coordinate[] array = new Coordinate[vertices.size()];
		
		for (int i = 0; i < array.length; i++) {
			VectorXZ vertex = vertices.get(i);
			array[i] = vectorXZToJTSCoordinate(vertex);
		}
		
		return new Polygon(
				new LinearRing(new CoordinateArraySequence(array), GF),
				null, GF);
		
	}
	
	public static final PolygonWithHolesXZ
		polygonXZFromJTSPolygon(Polygon polygon) {
		
		/* create outer polygon */
		
		SimplePolygonXZ outerPolygon =
			polygonXZFromLineString(polygon.getExteriorRing());
		
		/* create holes */
		
		List<SimplePolygonXZ> holes = Collections.emptyList();
		
		if (polygon.getNumInteriorRing() > 0) {
			holes = new ArrayList<SimplePolygonXZ>();
			for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
				holes.add(polygonXZFromLineString(
						polygon.getInteriorRingN(i)));
			}
		}
		
		return new PolygonWithHolesXZ(outerPolygon, holes);
		
	}

	private static final SimplePolygonXZ polygonXZFromLineString(LineString lineString) {
		
		List<VectorXZ> vertexLoop = new ArrayList<VectorXZ>(lineString.getNumPoints());
		
		for (Coordinate coordinate : lineString.getCoordinates()) {
			vertexLoop.add(vectorXZFromJTSCoordinate(coordinate));
		}
		
		return new SimplePolygonXZ(vertexLoop);
	}
	
	public static final Collection<PolygonWithHolesXZ>
		polygonsXZFromJTSGeometry(Geometry geometry) {
		
		Collection<PolygonWithHolesXZ> result =
			new ArrayList<PolygonWithHolesXZ>(1);
		
		if (geometry instanceof Polygon) {
			if (geometry.getNumPoints() > 2) {
				result.add(polygonXZFromJTSPolygon((Polygon)geometry));
			}
		} else if (geometry instanceof GeometryCollection) {
			GeometryCollection collection = (GeometryCollection)geometry;
			for (int i = 0; i < collection.getNumGeometries(); i++) {
				result.addAll(polygonsXZFromJTSGeometry(collection.getGeometryN(i)));
			}
		} else {
			System.err.println("unhandled geometry type: " + geometry.getClass());
		}
		
		return result;
		
	}
		
}