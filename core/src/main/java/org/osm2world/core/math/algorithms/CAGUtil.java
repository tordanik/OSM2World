package org.osm2world.core.math.algorithms;

import static org.osm2world.core.math.algorithms.FaceDecompositionUtil.splitPolygonIntoFaces;
import static org.osm2world.core.math.algorithms.JTSConversionUtil.polygonsFromJTS;
import static org.osm2world.core.math.algorithms.JTSConversionUtil.toJTS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Polygon;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.math.shapes.PolygonWithHolesXZ;
import org.osm2world.core.math.shapes.SimplePolygonXZ;

/**
 * utility class for Constructive Area Geometry (CAG),
 * boolean operations on areas
 */
public final class CAGUtil {

	private CAGUtil() {
	}

	/**
	 * takes a polygon outline, "subtracts" a collection of other polygons (which may themselves have holes),
	 * and returns a collection of polygons that covers the difference area.
	 * <p>
	 * The result polygons should cover the area that was within the original polygon,
	 * but not within a subtracted polygon.
	 *
	 * @return polygons without self-intersections, but maybe with holes
	 */
	public static final Collection<PolygonWithHolesXZ> subtractPolygons(
			PolygonShapeXZ basePolygon, List<? extends PolygonShapeXZ> subtractPolygons) {
		return splitPolygonIntoFaces(basePolygon, subtractPolygons, List.of());
	}

	private static final Collection<PolygonWithHolesXZ> subtractPolygonsWithJTS(
			PolygonShapeXZ basePolygon, List<? extends PolygonShapeXZ> subtractPolygons) {

		List<Geometry> remainingGeometry = Collections.singletonList(
				(Geometry)toJTS(basePolygon));

		for (PolygonShapeXZ subtractPolygon : subtractPolygons) {

			Polygon jtsSubtractPolygon = toJTS(subtractPolygon);

			if (!jtsSubtractPolygon.isValid()) continue;

			List<Geometry> newRemainingGeometry = new ArrayList<>(1);

			for (Geometry g : remainingGeometry) {

				Geometry newG = g.difference(jtsSubtractPolygon);

				if (newG instanceof GeometryCollection) {
					for (int i = 0; i < newG.getNumGeometries(); i++) {
						newRemainingGeometry.add(newG.getGeometryN(i));
					}
				} else {
					newRemainingGeometry.add(newG);
				}

			}

			remainingGeometry = newRemainingGeometry;

		}

		Collection<PolygonWithHolesXZ> result = new ArrayList<>();

		for (Geometry g : remainingGeometry) {
			result.addAll(polygonsFromJTS(g));
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

			Polygon jtsPoly = toJTS(poly);

			if (remainingGeometry == null) {
				remainingGeometry = jtsPoly;
			} else {
				remainingGeometry = remainingGeometry.intersection(jtsPoly);
			}

		}

		return polygonsFromJTS(remainingGeometry);

	}

}
