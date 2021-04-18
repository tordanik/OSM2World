package org.osm2world.core.math;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.math.shapes.PolylineShapeXZ;
import org.osm2world.core.math.shapes.SimplePolygonShapeXZ;

/**
 * converts between OSM2World's own geometry classes and the JTS geometry representations.
 *
 * When handling three-dimensional coordinates, the y and z information will
 * be swapped when converting from or to JTS. JTS uses x and y as 2D plane, z as elevation info.
 */
public class JTSConversionUtil {

	public static final GeometryFactory GF = new GeometryFactory();

	public static final Coordinate toJTS(VectorXZ v) {
		return new Coordinate(v.x, v.z);
	}

	public static final VectorXZ fromJTS(Coordinate c) {
		return new VectorXZ(c.x, c.y);
	}

	public static LineSegment toJTS(LineSegmentXZ segment) {
		return new LineSegment(toJTS(segment.p1), toJTS(segment.p2));
	}

	public static LineString toJTSLineString(PolylineShapeXZ polyline) {
		List<Coordinate> ps = polyline.vertices().stream().map(p -> toJTS(p)).collect(toList());
		return new LineString(new CoordinateArraySequence(ps.toArray(new Coordinate[0])), GF);
	}

	public static final Polygon toJTS(SimplePolygonShapeXZ polygon) {
		return new Polygon(toJTSLinearRing(polygon), null, GF);
	}

	public static final Polygon toJTS(PolygonShapeXZ polygon) {

		LinearRing shell = toJTSLinearRing(polygon.getOuter());

		LinearRing[] holes = polygon.getHoles().stream()
				.map(h -> toJTSLinearRing(h))
				.toArray(LinearRing[]::new);

		return new Polygon(shell, holes, GF);

	}

	private static final LinearRing toJTSLinearRing(SimplePolygonShapeXZ polygon) {

		List<VectorXZ> vertices = polygon.vertices();

		Coordinate[] array = new Coordinate[vertices.size()];

		for (int i = 0; i < array.length; i++) {
			array[i] = toJTS(vertices.get(i));
		}

		return new LinearRing(new CoordinateArraySequence(array), GF);

	}

	public static final PolygonWithHolesXZ fromJTS(Polygon polygon) {

		/* create outer polygon */

		SimplePolygonXZ outerPolygon = polygonFromJTS(polygon.getExteriorRing());

		/* create holes */

		List<SimplePolygonXZ> holes = emptyList();

		if (polygon.getNumInteriorRing() > 0) {
			holes = new ArrayList<>();
			for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
				holes.add(polygonFromJTS(polygon.getInteriorRingN(i)));
			}
		}

		return new PolygonWithHolesXZ(outerPolygon, holes);

	}

	private static final SimplePolygonXZ polygonFromJTS(LineString lineString) {

		List<VectorXZ> vertexLoop = new ArrayList<>(lineString.getNumPoints());

		for (Coordinate coordinate : lineString.getCoordinates()) {
			vertexLoop.add(fromJTS(coordinate));
		}

		return new SimplePolygonXZ(vertexLoop);
	}

	public static final List<PolygonWithHolesXZ> polygonsFromJTS(Geometry geometry) {

		List<PolygonWithHolesXZ> result = new ArrayList<>(1);

		if (geometry instanceof Polygon) {
			if (geometry.getNumPoints() > 2) {
				try {
					result.add(fromJTS((Polygon)geometry));
				} catch (InvalidGeometryException e) {
					System.err.println("Ignoring invalid JTS polygon: " + e.getMessage());
				}
			}
		} else if (geometry instanceof GeometryCollection) {
			GeometryCollection collection = (GeometryCollection)geometry;
			for (int i = 0; i < collection.getNumGeometries(); i++) {
				result.addAll(polygonsFromJTS(collection.getGeometryN(i)));
			}
		} else {
			System.err.println("unhandled geometry type: " + geometry.getClass());
		}

		return result;

	}

}