package org.osm2world.core.math.shapes;

import static java.lang.Math.max;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.math.VectorXZ.distance;

import java.util.Comparator;
import java.util.List;

import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.VectorXZ;

/**
 * a polyline (aka linestring) with at least two points.
 * This is an open shape, i.e. it does not represent a non-zero area.
 */
public interface PolylineShapeXZ extends ShapeXZ {

	/**
	 * returns the length of the entire polyline
	 */
	public double getLength();

	/**
	 * returns the ordered list of segments between the vertices
	 */
	List<LineSegmentXZ> getSegments();

	/**
	 * returns the length between the start of this polyline and a given point on it,
	 * measured along this polyline.
	 *
	 * @param point  the point, must lie on this polyline (but does not have to be a vertex)
	 * @throws IllegalArgumentException  if the point is not on the polyline
	 */
	default public double offsetOf(VectorXZ point) {

		List<VectorXZ> vertices = getVertexList();

		// how far a point can be from a segment of this polyline and still be considered "on" it
		final double IS_ON_TOLERANCE = 1e-4;

		/* if the point is a vertex of this polyline ... */

		int pointIndex = vertices.indexOf(point);

		if (pointIndex != -1) {

			return new PolylineXZ(vertices.subList(0, pointIndex + 1)).getLength();

		}

		/* else, if the point is on one of the segments between the vertices ... */

		List<LineSegmentXZ> segments = getSegments();

		for (int i = 0; i < segments.size(); i++) {

			LineSegmentXZ segment = segments.get(i);

			if (distanceFromLine(point, segment.p1, segment.p2) <= IS_ON_TOLERANCE) {

				double offset = 0;

				if (i > 0) {
					offset += new PolylineXZ(vertices.subList(0, i + 1)).getLength();
				}

				offset += distance(segment.p1, point);

				return offset;

			}

		}

		/* the point is not on this polyline! */

		throw new IllegalArgumentException("point " + point + " is not on polyline " + this);

	}

	/**
	 * returns the point at a given distance from the start of this polyline.
	 * This is, semantically, the inverse of {@link #offsetOf(VectorXZ)}.
	 *
	 * @param offset  the offset, must be between 0 and {@link #getLength()}
	 * @return        the point at the offset, != null
	 */
	default public VectorXZ pointAtOffset(double offset) {

		assert offset >= 0 && offset <= getLength();

		List<LineSegmentXZ> segments = getSegments();

		for (int i = 0; i < segments.size(); i++) {

			LineSegmentXZ segment = segments.get(i);

			if (offset <= segment.getLength()) {
				return interpolateBetween(segment.p1, segment.p2,
						max(0, offset / segment.getLength()));
			}

			offset -= segment.getLength();

		}

		/* return the last vertex: this might happen to accumulating floating point
		 * errors if the offset is (almost) the same as this polyline's length */

		return getVertexList().get(getVertexList().size() - 1);

	}

	/** returns the point on this shape that is closest to the parameter */
	default public VectorXZ closestPoint(VectorXZ p) {
		return getSegments().stream()
				.map(s -> s.closestPoint(p))
				.min(Comparator.comparing(p::distanceTo))
				.get();
	}
}
