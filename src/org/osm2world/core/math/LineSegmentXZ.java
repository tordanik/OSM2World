package org.osm2world.core.math;

public class LineSegmentXZ {

	public final VectorXZ p1, p2;

	public LineSegmentXZ(VectorXZ p1, VectorXZ p2) {
		this.p1 = p1;
		this.p2 = p2;
	}

	public VectorXZ getCenter() {
		return GeometryUtil.interpolateBetween(p1, p2, 0.5);
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
	public double getLength() {
		return VectorXZ.distance(p1, p2);
	}

}
