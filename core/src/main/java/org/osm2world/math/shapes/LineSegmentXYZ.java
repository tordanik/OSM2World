package org.osm2world.math.shapes;

import org.osm2world.math.VectorXYZ;

public class LineSegmentXYZ {

	public final VectorXYZ p1, p2;

	public LineSegmentXYZ(VectorXYZ p1, VectorXYZ p2) {
		this.p1 = p1;
		this.p2 = p2;
	}

	@Override
	public String toString() {
		return "[" + p1 + ", " + p2 + "]";
	}

	public LineSegmentXZ getSegmentXZ() {
		return new LineSegmentXZ(p1.xz(), p2.xz());
	}

	/** returns the flipped version of this segment */
	public LineSegmentXYZ reverse() {
		return new LineSegmentXYZ(p2, p1);
	}

	@Override
	public final boolean equals(Object o) {
		if (!(o instanceof LineSegmentXYZ that)) return false;
		return p1.equals(that.p1) && p2.equals(that.p2);
	}

	@Override
	public int hashCode() {
		int result = p1.hashCode();
		result = 31 * result + p2.hashCode();
		return result;
	}

}
