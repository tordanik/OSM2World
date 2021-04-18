package org.osm2world.core.math;

import static java.lang.Math.abs;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.osm2world.core.math.JTSConversionUtil.*;

import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.osm2world.core.math.shapes.PolylineShapeXZ;
import org.osm2world.core.math.shapes.ShapeXZ;

public class LineSegmentXZ implements PolylineShapeXZ {

	public final VectorXZ p1, p2;

	public LineSegmentXZ(VectorXZ p1, VectorXZ p2) {
		//TODO if (p1.equals(p2)) throw new IllegalArgumentException("points need to be different");
		this.p1 = p1;
		this.p2 = p2;
	}

	/**
	 * returns a list containing the two vertices {@link #p1} and {@link #p2}
	 */
	@Override
	public List<VectorXZ> vertices() {
		return asList(p1, p2);
	}

	@Override
	public List<LineSegmentXZ> getSegments() {
		return singletonList(this);
	}

	public VectorXZ getCenter() {
		return new VectorXZ((p1.x + p2.x) / 2, (p1.z + p2.z) / 2);
	}

	/** returns a normalized vector indicating the segment's direction */
	public VectorXZ getDirection() {
		return p2.subtract(p1).normalize();
	}

	/**
	 * returns true if there is an intersection between this line segment and
	 * the line segment defined by the parameters
	 */
	public boolean intersects(VectorXZ segmentP1, VectorXZ segmentP2) {
		// TODO: (performance): passing "vector TO second point", rather than
		// point2, would avoid having to calc it here - and that information
		// could be reused for all comparisons involving the segment
		return getIntersection(segmentP1, segmentP2) != null;
	}

	/**
	 * returns the intersection between this line segment and
	 * the line segment defined by the parameters;
	 * null if none exists.
	 */
	public VectorXZ getIntersection(VectorXZ segmentP1, VectorXZ segmentP2) {
		// TODO: (performance): passing "vector TO second point", rather than
		// point2, would avoid having to calc it here - and that information
		// could be reused for all comparisons involving the segment

		return GeometryUtil.getTrueLineSegmentIntersection(
				segmentP1, segmentP2, p1, p2);
	}

	/**
	 * returns the distance between this segment's two end nodes
	 */
	@Override
	public double getLength() {
		return VectorXZ.distance(p1, p2);
	}

	/** returns the flipped version of this segment */
	@Override
	public LineSegmentXZ reverse() {
		return new LineSegmentXZ(p2, p1);
	}

	@Override
	public ShapeXZ shift(VectorXZ moveVector) {
		return new LineSegmentXZ(p1.add(moveVector), p2.add(moveVector));
	}

	/** returns the point on this segment that is closest to the parameter */
	@Override
	public VectorXZ closestPoint(VectorXZ p) {
		LineSegment jtsSegment = JTSConversionUtil.toJTS(this);
		Coordinate jtsResult = jtsSegment.closestPoint(toJTS(p));
		return fromJTS(jtsResult);
	}

	/** returns the z value associated with a given x value so that the point (x, z) is on the line */
	public double evaluateAtX(double x) {
		double xLength = abs(p2.x - p1.x);
		if (xLength == 0) {
			return p1.z;
		} else {
			return p1.add((p2.subtract(p1)).mult(abs(x - p1.x) / xLength)).z;
		}
	}

	@Override
	public String toString() {
		return "[" + p1 + ", " + p2 + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((p1 == null) ? 0 : p1.hashCode());
		result = prime * result + ((p2 == null) ? 0 : p2.hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj) {

		if (obj instanceof LineSegmentXZ) {
			LineSegmentXZ other = (LineSegmentXZ) obj;
			return p1.equals(other.p1) && p2.equals(other.p2);
		} else {
			return false;
		}

	}

}
