package org.osm2world.math.algorithms;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.osm2world.conversion.ConversionLog;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.*;
import org.osm2world.util.exception.InvalidGeometryException;

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

		if (geometry instanceof Polygon polygon) {
			if (polygon.getNumPoints() > 2) {
				try {
					result.add(fromJTS(polygon));
				} catch (InvalidGeometryException e) {
					ConversionLog.warn("Ignoring invalid JTS polygon: " + e.getMessage(), e);
				}
			}
		} else if (geometry instanceof GeometryCollection collection) {
			for (int i = 0; i < collection.getNumGeometries(); i++) {
				result.addAll(polygonsFromJTS(collection.getGeometryN(i)));
			}
		} else if (!(geometry instanceof LineString) && !(geometry instanceof Point)) {
			// LineString and Point are known to sometimes occur in the result and are ignored; others are unexpected
			throw new Error("unhandled geometry type: " + geometry.getClass());
		}

		return result;

	}

}