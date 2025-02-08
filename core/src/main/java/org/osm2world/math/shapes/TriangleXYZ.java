package org.osm2world.math.shapes;

import java.util.Collection;
import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.GeometryUtil;
import org.osm2world.util.exception.InvalidGeometryException;

import com.google.common.collect.ImmutableList;

/**
 * immutable 3D triangle
 */
public class TriangleXYZ implements FlatSimplePolygonShapeXYZ {

	public final VectorXYZ v1, v2, v3;

	public TriangleXYZ(VectorXYZ v1, VectorXYZ v2, VectorXYZ v3) {

		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;

		if (getArea() < 1e-6) {
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
	 * returns the line segments connecting each successive pair of vertices.
	 * The order is always (v1, v2), (v2, v3), (v3, v1).
	 */
	public List<LineSegmentXYZ> segments() {
		return List.of(
				new LineSegmentXYZ(v1, v2),
				new LineSegmentXYZ(v2, v3),
				new LineSegmentXYZ(v3, v1)
		);
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

	public TriangleXYZ rotateY(double angleRad) {
		return new TriangleXYZ(v1.rotateY(angleRad), v2.rotateY(angleRad), v3.rotateY(angleRad));
	}

	public TriangleXYZ scale(VectorXYZ scaleOrigin, double scaleFactor) {
		TriangleXYZ t = this.shift(scaleOrigin.invert());
		t = new TriangleXYZ(t.v1.mult(scaleFactor), t.v2.mult(scaleFactor), t.v3.mult(scaleFactor));
		return t.shift(scaleOrigin);
	}

	/**
	 * returns the projection of this triangle into XZ plane.
	 * Fails if this triangle is vertical.
	 */
	public TriangleXZ xz() {
		return new TriangleXZ(v1.xz(), v2.xz(), v3.xz());
	}

	@Override
	public String toString() {
		return "[" + v1 + ", " + v2 + ", " + v3 + "]";
	}

	/**
	 * splits this triangle where it intersects a plane.
	 * The splitting plane is defined by a line in the XZ plane and expands vertically up and down from it.
	 * If the line does not split this triangle, the unaltered triangle is returned.
	 */
	public Collection<TriangleXYZ> split(LineXZ l) {

		/* find intersections between the line and each of the triangle's sides */

		List<LineSegmentXYZ> segments = this.segments();
		VectorXYZ[] intersections = new VectorXYZ[3];
		int numIntersections = 0;

		for (int i = 0; i < 3; i++) {
			LineSegmentXYZ segment = segments.get(i);
			if (!segment.p1.xz().equals(segment.p2.xz())) {
				VectorXZ intersectionXZ = l.getIntersection(segment.getSegmentXZ());
				if (intersectionXZ != null) {
					double ratio = segment.p1.distanceToXZ(intersectionXZ) / segment.getSegmentXZ().getLength();
					intersections[i] = GeometryUtil.interpolateBetween(segment.p1, segment.p2, ratio);
					numIntersections++;
				}
			}
		}

		/* check if any points are (almost) exactly on the line and handle those special cases */

		boolean v1OnLine = l.distanceToXZ(v1) < 0.001;
		boolean v2OnLine = l.distanceToXZ(v2) < 0.001;
		boolean v3OnLine = l.distanceToXZ(v3) < 0.001;

		if (v1OnLine && intersections[1] != null) {
			return List.of(
					new TriangleXYZ(v1, v2, intersections[1]),
					new TriangleXYZ(v3, v1, intersections[1])
			);
		} if (v2OnLine && intersections[2] != null) {
			return List.of(
					new TriangleXYZ(v2, v3, intersections[2]),
					new TriangleXYZ(v1, v2, intersections[2])
			);
		} else if (v3OnLine && intersections[0] != null) {
			return List.of(
					new TriangleXYZ(v3, v1, intersections[0]),
					new TriangleXYZ(v2, v3, intersections[0])
			);
		}

		/* handle cases where the line only touches the triangle or passes by it entirely */

		if (numIntersections != 2) {
			return List.of(this);
		}

		/* common case where the line intersects two sides: split the triangle at the two intersections */

		if (intersections[0] == null) {
			return List.of(
					new TriangleXYZ(v3, intersections[2], intersections[1]),
					new TriangleXYZ(v1, intersections[1], intersections[2]),
					new TriangleXYZ(v2, intersections[1], v1)
			);
		} else if (intersections[1] == null) {
			return List.of(
					new TriangleXYZ(v1, intersections[0], intersections[2]),
					new TriangleXYZ(v2, intersections[2], intersections[0]),
					new TriangleXYZ(v3, intersections[2], v2)
			);
		} else {
			assert intersections[2] == null;
			return List.of(
					new TriangleXYZ(v2, intersections[1], intersections[0]),
					new TriangleXYZ(v3, intersections[0], intersections[1]),
					new TriangleXYZ(v1, intersections[0], v3)
			);
		}

	}

}
