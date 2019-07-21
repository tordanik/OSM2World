package org.osm2world.core.math.shapes;

import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.math.VectorXZ.distance;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.VectorXZ;

/**
 * a polyline (aka linestring)
 */
public class PolylineXZ implements PolylineShapeXZ {

	/** how far a point can be from a segment of this polyline and still be considered "on" it */
	private static final double IS_ON_TOLERANCE = 1e-4;

	private final List<VectorXZ> vertices;

	public PolylineXZ(List<VectorXZ> vertices) {
		this.vertices = vertices;
	}

	public PolylineXZ(VectorXZ... vertices) {
		this(asList(vertices));
	}

	@Override
	public List<VectorXZ> getVertexList() {
		return vertices;
	}

	@Override
	public List<LineSegmentXZ> getSegments() {

		List<LineSegmentXZ> segments = new ArrayList<LineSegmentXZ>(vertices.size() - 1);

		for (int i=0; i+1 < vertices.size(); i++) {
			segments.add(new LineSegmentXZ(vertices.get(i), vertices.get(i+1)));
		}

		return segments;

	}

	@Override
	public double getLength() {

		double length = 0;

		for (int i = 0; i + 1 < vertices.size(); i++) {
			length += distance(vertices.get(i), vertices.get(i+1));
		}

		return length;

	}

	/**
	 * returns the length between the start of this polyline and a given point on it
	 *
	 * @param point  the point, must lie on this polyline (but does not have to be a vertex)
	 */
	public double offsetOf(VectorXZ point) {

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
	 * returns the point at a given distance from the start of this linestring.
	 * This is, semantically, the inverse of #offsetOf.
	 *
	 * @param offset  the offset, must be between 0 and this segment's length
	 * @return        the point at the offset, != null
	 */
	public VectorXZ pointAtOffset(double offset) {

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

		return vertices.get(vertices.size() - 1);

	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PolylineXZ) {
			return vertices.equals(((PolylineXZ)obj).vertices);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return vertices.hashCode();
	}

	@Override
	public String toString() {
		return vertices.toString();
	}

}
