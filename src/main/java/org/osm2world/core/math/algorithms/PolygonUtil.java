package org.osm2world.core.math.algorithms;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Double.NaN;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Comparator.comparingDouble;
import static org.osm2world.core.math.GeometryUtil.isRightOf;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;

/**
 * utility class for basic polygon-related algorithms
 */
public class PolygonUtil {

	/** prevents instantiation */
	private PolygonUtil() { }

	/**
	 * returns the convex hull of a simple polygon.
	 *
	 * @param polygon  any simple polygon, != null
	 * @return  the convex hull. Its points are ordered as they were in the original polygon.
	 */
	public static final SimplePolygonXZ convexHull(SimplePolygonXZ polygon) {

		List<VectorXZ> vertices = polygon.makeClockwise().getVertices();

		/* determine points with min/max x value (guaranteed to be in convex hull) */

		VectorXZ minV = min(vertices, comparingDouble(v -> v.x));
		VectorXZ maxV = max(vertices, comparingDouble(v -> v.x));

		int minI = vertices.indexOf(minV);
		int maxI = vertices.indexOf(maxV);

		/* split the polygon into an upper and lower "half" at the two points */

		List<VectorXZ> upperHalf = new ArrayList<VectorXZ>();
		List<VectorXZ> lowerHalf = new ArrayList<VectorXZ>();

		upperHalf.add(minV);

		for (int i = (minI + 1) % vertices.size(); i != maxI; i = (i+1) % vertices.size()) {
			upperHalf.add(vertices.get(i));
		}

		upperHalf.add(maxV);

		lowerHalf.add(maxV);

		for (int i = (maxI + 1) % vertices.size(); i != minI; i = (i+1) % vertices.size()) {
			lowerHalf.add(vertices.get(i));
		}

		lowerHalf.add(minV);

		/* perform the calculation for each of the two parts */

		List<VectorXZ> upperResult = convexHullPart(upperHalf);
		List<VectorXZ> lowerResult = convexHullPart(lowerHalf);

		/* combine the results */

		upperResult.addAll(lowerResult.subList(1, lowerResult.size()));

		if (!polygon.isClockwise()) {
			reverse(upperResult);
		}

		return new SimplePolygonXZ(upperResult);

	}

	/**
	 * calculates the convex hull partially for the upper or lower "half"
	 * of a polygon. Used in {@link #convexHull(SimplePolygonXZ)}.
	 */
	private static List<VectorXZ> convexHullPart(List<VectorXZ> vertices) {

		checkArgument(vertices.size() >= 2);

		if (vertices.size() < 3) {
			return vertices;
		}

		// preliminary result, vertices can be removed from its end at a later point
		List<VectorXZ> result = new ArrayList<VectorXZ>();

		result.add(vertices.get(0));
		result.add(vertices.get(1));

		for (int i = 2; i < vertices.size(); i++) {

			VectorXZ v = vertices.get(i);

			while (result.size() > 1) {

				if (isRightOf(result.get(result.size() - 2), v,
						result.get(result.size() - 1))) {

					result.remove(result.size() - 1);

				} else {
					break;
				}

			}

			result.add(v);

		}

		return result;

	}

	/**
	 * Calculates the smallest possible bounding box for this polygon.
	 * The result is not (generally) an axis-aligned bounding box!
	 *
	 * Relies on the fact that one side of the box must be collinear with
	 * one of the sides of the polygon's convex hull.
	 *
	 * @param polygon  any simple polygon, != null
	 * @return  a simple polygon with exactly 4 vertices, representing the box
	 */
	public static final SimplePolygonXZ minimumBoundingBox(SimplePolygonXZ polygon) {

		/*
		 * For each side of the polygon, rotate the polygon to make that side
		 * parallel to the Z axis, then calculate the axis aligned bounding box.
		 * These are the candidate boxes for minimum area.
		 */

		AxisAlignedBoundingBoxXZ minBox = null;
		double angleForMinBox = NaN;

		for (int i = 0; i < polygon.size(); i++) {

			double angle = polygon.getVertex(i).angleTo(polygon.getVertexAfter(i));

			List<VectorXZ> rotatedVertices = new ArrayList<VectorXZ>();
			for (VectorXZ v : polygon.getVertexCollection()) {
				rotatedVertices.add(v.rotate(-angle));
			}

			AxisAlignedBoundingBoxXZ box = new AxisAlignedBoundingBoxXZ(rotatedVertices);

			if (minBox == null || box.area() < minBox.area()) {
				minBox = box;
				angleForMinBox = angle;
			}

		}

		/* construct the result */

		return new SimplePolygonXZ(asList(
				minBox.bottomLeft().rotate(angleForMinBox),
				minBox.bottomRight().rotate(angleForMinBox),
				minBox.topRight().rotate(angleForMinBox),
				minBox.topLeft().rotate(angleForMinBox),
				minBox.bottomLeft().rotate(angleForMinBox)));

	}

}
