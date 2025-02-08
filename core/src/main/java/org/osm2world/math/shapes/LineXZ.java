package org.osm2world.math.shapes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osm2world.math.Vector3D;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.GeometryUtil;

/** an infinite line in the XZ plane */
public record LineXZ(@Nonnull VectorXZ p1, @Nonnull VectorXZ p2) {

	public LineXZ {
		if (p1.equals(p2)) {
			throw new IllegalArgumentException("Points defining a line must be distinct");
		}
	}

	/** returns a normalized vector indicating the line's direction */
	public VectorXZ getDirection() {
		return p2.subtract(p1).normalize();
	}

	public @Nullable VectorXZ getIntersection(LineXZ l) {
		return GeometryUtil.getLineIntersection(p1, getDirection(), l.p1, l.getDirection());
	}

	public @Nullable VectorXZ getIntersection(LineSegmentXZ l) {
		@Nullable VectorXZ intersection = GeometryUtil.getLineIntersection(p1, getDirection(), l.p1, l.getDirection());
		if (intersection != null && GeometryUtil.isBetween(intersection, l.p1, l.p2)) {
			return intersection;
		}
		return null;
	}

	/** returns the closest distance between p and this line */
	public double distanceTo(VectorXZ v) {
		return GeometryUtil.distanceFromLine(v, p1, p2);
	}

	public double distanceToXZ(Vector3D v) {
		return GeometryUtil.distanceFromLine(v.xz(), p1, p2);
	}

	@Override
	public String toString() {
		return "[" + p1 + ", " + p2 + "]";
	}

}
