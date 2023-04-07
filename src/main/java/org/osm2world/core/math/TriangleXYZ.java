package org.osm2world.core.math;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class TriangleXYZ implements FlatSimplePolygonShapeXYZ {

	public final VectorXYZ v1, v2, v3;

	public TriangleXYZ(VectorXYZ v1, VectorXYZ v2, VectorXYZ v3) {

		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;

		if (!v1.isFinite() || !v2.isFinite() || !v3.isFinite()) {
			throw new InvalidGeometryException("Triangle vertex is not finite: " + v1 + ", " + v2 + ", " + v3);
		} else if (getArea() < 1e-6) {
			// degenerate triangle: all three points are (almost, to account for floating point arithmetic) in a line
			throw new InvalidGeometryException("Degenerate triangle: " + v1 + ", " + v2 + ", " + v3);
		}

	}

	@Override
	public List<VectorXYZ> verticesNoDup() {
		return ImmutableList.of(v1, v2, v3);
	}

	@Override
	public List<VectorXYZ> vertices() {
		return ImmutableList.of(v1, v2, v3, v1);
	}

	/**
	 * returns the normalized normal vector of this triangle.
	 * Points "up" based on assumption that this is a counterclockwise triangle.
	 */
	@Override
	public VectorXYZ getNormal() {
		return v2.subtract(v1).crossNormalized(v2.subtract(v3));
	}

	public VectorXYZ getCenter() {
		return new VectorXYZ(
				(v1.x + v2.x + v3.x) / 3,
				(v1.y + v2.y + v3.y) / 3,
				(v1.z + v2.z + v3.z) / 3);
	}

	/**
	 * returns the triangle's y coord value at a {@link VectorXZ} within the triangle's 2D footprint.
	 * It is obtained by linear interpolation within the triangle.
	 */
	@Override
	public double getYAt(VectorXZ pos) {

		double a = v1.z * (v2.y - v3.y) + v2.z * (v3.y - v1.y) + v3.z * (v1.y - v2.y);
		double b = v1.y * (v2.x - v3.x) + v2.y * (v3.x - v1.x) + v3.y * (v1.x - v2.x);
		double c = v1.x * (v2.z - v3.z) + v2.x * (v3.z - v1.z) + v3.x * (v1.z - v2.z);
		double d = -a * v1.x - b * v1.z - c * v1.y;

		return -a/c * pos.x - b/c * pos.z - d/c;

	}

	/**
	 * returns the area of the triangle
	 */
	public double getArea() {

		VectorXYZ w1 = v2.subtract(v1);
		VectorXYZ w2 = v3.subtract(v1);

		return 0.5 * (w1.cross(w2)).length();

	}

	/** creates a new triangle by adding a shift vector to each vertex of this triangle */
	public TriangleXYZ shift(VectorXYZ v) {
		return new TriangleXYZ(v1.add(v), v2.add(v), v3.add(v));
	}

	@Override
	public String toString() {
		return "[" + v1 + ", " + v2 + ", " + v3 + "]";
	}

}