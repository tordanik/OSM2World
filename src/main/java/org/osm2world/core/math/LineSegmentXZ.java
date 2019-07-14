package org.osm2world.core.math;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.util.List;

import org.osm2world.core.math.shapes.PolylineShapeXZ;

public class LineSegmentXZ implements PolylineShapeXZ {

	public final VectorXZ p1, p2;

	public LineSegmentXZ(VectorXZ p1, VectorXZ p2) {
		this.p1 = p1;
		this.p2 = p2;
	}

	/**
	 * returns a list containing the two vertices {@link #p1} and {@link #p2}
	 */
	@Override
	public List<VectorXZ> getVertexList() {
		return asList(p1, p2);
	}
	
	@Override
	public List<LineSegmentXZ> getSegments() {
		return singletonList(this);
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
	
	/**
	 * produces the flipped version of this segment
	 */
	public LineSegmentXZ reverse() {
		return new LineSegmentXZ(p2, p1);
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
