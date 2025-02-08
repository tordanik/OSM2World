package org.osm2world.math.shapes;

import org.osm2world.math.VectorXYZ;

public class LineSegmentXYZ {

	public final VectorXYZ p1, p2;

	public LineSegmentXYZ(VectorXYZ p1, VectorXYZ p2) {
		this.p1 = p1;
		this.p2 = p2;
	}

	public LineSegmentXZ getSegmentXZ() {
		return new LineSegmentXZ(p1.xz(), p2.xz());
	}

	@Override
	public String toString() {
		return "[" + p1 + ", " + p2 + "]";
	}

}
