package org.osm2world.math.shapes;

import static java.lang.Double.isFinite;
import static java.util.Collections.singletonList;

import java.util.List;
import java.util.function.Function;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.GeometryUtil;

import com.google.common.collect.ImmutableList;

/**
 * immutable 2D triangle
 */
public class TriangleXZ implements SimplePolygonShapeXZ {

	public final VectorXZ v1, v2, v3;

	public TriangleXZ(VectorXZ v1, VectorXZ v2, VectorXZ v3) {

		assert v1 != null && v2 != null && v3 != null;

		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;

	}

	@Override
	public List<VectorXZ> vertices() {
		return ImmutableList.of(v1, v2, v3, v1);
	}

	public List<VectorXZ> getVertices() {
		return ImmutableList.of(v1, v2, v3);
	}

	public VectorXZ getCenter() {
		return new VectorXZ(
				(v1.x + v2.x + v3.x) / 3,
				(v1.z + v2.z + v3.z) / 3);
	}

	@Override
	public VectorXZ getCentroid() {
		return getCenter();
	}

	public TriangleXYZ xyz(double y) {
		return new TriangleXYZ(v1.xyz(y), v2.xyz(y), v3.xyz(y));
	}

	public TriangleXYZ xyz(Function<VectorXZ, VectorXYZ> xyzFunction) {
		return new TriangleXYZ(
				xyzFunction.apply(v1),
				xyzFunction.apply(v2),
				xyzFunction.apply(v3));
	}

	@Override
	public boolean isClockwise() {
		return GeometryUtil.isRightOf(v3, v1, v2);
	}

	/**
	 * returns this triangle if it is counterclockwise,
	 * or the reversed triangle if it is clockwise.
	 */
	public TriangleXZ makeClockwise() {
		return makeRotationSense(true);
	}

	/**
	 * returns this triangle if it is clockwise,
	 * or the reversed triangle if it is counterclockwise.
	 */
	public TriangleXZ makeCounterclockwise() {
		return makeRotationSense(false);
	}

	private TriangleXZ makeRotationSense(boolean clockwise) {
		if (isClockwise() ^ clockwise) {
			return this.reverse();
		} else {
			return this;
		}
	}

	/**
	 * returns the area of the triangle
	 */
	@Override
	public double getArea() {

		double sum =
				+ v1.x * v2.z
				- v2.x * v1.z
				+ v2.x * v3.z
				- v3.x * v2.z
				+ v3.x * v1.z
				- v1.x * v3.z;

		return Math.abs(sum / 2);

	}

	/**
	 * returns an inversed version of this triangle.
	 * It consists of the same vertices, but has the other direction.
	 */
	public TriangleXZ reverse() {
		return new TriangleXZ(v3, v2, v1);
	}

	@Override
	public SimplePolygonShapeXZ convexHull() {
		return this;
	}

	@Override
	public List<TriangleXZ> getTriangulation() {
		return singletonList(this);
	}

	/**
	 * checks if the triangle contains NaN/Infinity values or is degenerate. That is, all three points are
	 * (almost, to account for floating point arithmetic) in a line.
	 */
	public boolean isDegenerateOrNaN() {
		return getArea() < 1e-5
				|| !isFinite(v1.x) || !isFinite(v1.z)
				|| !isFinite(v2.x) || !isFinite(v2.z)
				|| !isFinite(v3.x) || !isFinite(v3.z);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + v1.hashCode();
		result = prime * result + v2.hashCode();
		result = prime * result + v3.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		} else if (!(obj instanceof TriangleXZ)) {
			return false;
		} else {
			TriangleXZ other = (TriangleXZ)obj;
			return v1.equals(other.v1)
					&& v2.equals(other.v2)
					&& v3.equals(other.v3);
		}

	}

	@Override
	public String toString() {
		return "[" + v1 + ", " + v2 + ", " + v3 + "]";
	}

}
