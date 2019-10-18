package org.osm2world.core.math.algorithms;

import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.JTSConversionUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Polygon;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;

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
	 * The result polygons should cover the area that was within the original polygon,
	 * but not within a subtracted polygon.
	 *
	 * @return  polygons without self-intersections, but maybe with holes
	 */
	public static final Collection<PolygonWithHolesXZ> subtractPolygons(
			SimplePolygonXZ basePolygon,
			List<? extends SimplePolygonXZ> subtractPolygons) {

		return subtractPolygonsWithHolesXZ(basePolygon,
				subtractPolygons.stream().map(p -> p.asPolygonWithHolesXZ()).collect(toList()));

	}

	/**
	 * variant of {@link #subtractPolygons(SimplePolygonXZ, List)} which accepts polygons with holes
	 * TODO make obsolete by turning simple polygons into a subtype of complex polygons
	 */
	public static final Collection<PolygonWithHolesXZ> subtractPolygonsWithHolesXZ(
			SimplePolygonXZ basePolygon,
			List<? extends PolygonWithHolesXZ> subtractPolygons) {

		List<Geometry> remainingGeometry = Collections.singletonList(
				(Geometry)polygonXZToJTSPolygon(basePolygon));

		for (PolygonWithHolesXZ subtractPolygon : subtractPolygons) {

			Polygon jtsSubtractPolygon = polygonXZToJTSPolygon(subtractPolygon);

			if (!jtsSubtractPolygon.isValid()) continue;

			List<Geometry> newRemainingGeometry = new ArrayList<Geometry>(1);

			for (Geometry g : remainingGeometry) {

				Geometry newG = g.difference(jtsSubtractPolygon);

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
