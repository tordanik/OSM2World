package org.osm2world.math.shapes;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.Collection;
import java.util.List;

import org.osm2world.math.Vector3D;
import org.osm2world.math.VectorXYZ;

/**
 * immutable representation of an axis-aligned bounding box
 * with x, y and z dimensions
 */
public class AxisAlignedBoundingBoxXYZ {

	public final double minX, minY, minZ, maxX, maxY, maxZ;

	public AxisAlignedBoundingBoxXYZ(double minX, double minY, double minZ,
			double maxX, double maxY, double maxZ) {
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxY = maxY;
		this.maxZ = maxZ;
	}

	/**
	 * @param boundedPoints  must contain at least one point
	 */
	public AxisAlignedBoundingBoxXYZ(Collection<? extends Vector3D> boundedPoints) {

		assert (!boundedPoints.isEmpty());

		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

		for (Vector3D p : boundedPoints) {
			minX = min(minX, p.getX()); minY = min(minY, p.getY()); minZ = min(minZ, p.getZ());
			maxX = max(maxX, p.getX()); maxY = max(maxY, p.getY()); maxZ = max(maxZ, p.getZ());
		}

		this.minX = minX; this.minY = minY; this.minZ = minZ;
		this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;

	}

	public double sizeX() { return maxX - minX; }
	public double sizeY() { return maxY - minY; }
	public double sizeZ() { return maxZ - minZ; }

	public VectorXYZ center() {
		return new VectorXYZ(minX + sizeX()/2, minY + sizeY()/2, minZ + sizeZ()/2);
	}

	public Collection<VectorXYZ> corners() {
		return List.of(
				new VectorXYZ(minX, minY, minZ),
				new VectorXYZ(minX, minY, maxZ),
				new VectorXYZ(minX, maxY, minZ),
				new VectorXYZ(minX, maxY, maxZ),
				new VectorXYZ(maxX, minY, minZ),
				new VectorXYZ(maxX, minY, maxZ),
				new VectorXYZ(maxX, maxY, minZ),
				new VectorXYZ(maxX, maxY, maxZ));
	}

	/**
	 * returns a bounding box that is a bit larger than this one
	 */
	public AxisAlignedBoundingBoxXYZ pad(double paddingSize) {
		return new AxisAlignedBoundingBoxXYZ(
				minX - paddingSize,
				minY - paddingSize,
				minZ - paddingSize,
				maxX + paddingSize,
				maxY + paddingSize,
				maxZ + paddingSize);
	}

	public boolean overlaps(AxisAlignedBoundingBoxXYZ otherBox) {
		return !(maxX <= otherBox.minX
				|| minX >= otherBox.maxX
				|| maxY <= otherBox.minY
				|| minY >= otherBox.maxY
				|| maxZ <= otherBox.minZ
				|| minZ >= otherBox.maxZ);
	}

	public boolean contains(AxisAlignedBoundingBoxXYZ otherBox) {
		return minX <= otherBox.minX
				&& minY <= otherBox.minY
				&& minZ <= otherBox.minZ
				&& maxX >= otherBox.maxX
				&& maxY >= otherBox.maxY
				&& maxZ >= otherBox.maxZ;
	}

	public boolean contains(VectorXYZ v) {
		return v.x >= minX && v.x <= maxX && v.y >= minY && v.y <= maxY && v.z >= minZ && v.z <= maxZ;
	}

	public AxisAlignedRectangleXZ xz() {
		return new AxisAlignedRectangleXZ(minX, minZ, maxX, maxZ);
	}

	public static final AxisAlignedBoundingBoxXYZ union(
			AxisAlignedBoundingBoxXYZ box1, AxisAlignedBoundingBoxXYZ box2) {

		return new AxisAlignedBoundingBoxXYZ(
				Math.min(box1.minX, box2.minX),
				Math.min(box1.minY, box2.minY),
				Math.min(box1.minZ, box2.minZ),
				Math.max(box1.maxX, box2.maxX),
				Math.max(box1.maxY, box2.maxY),
				Math.max(box1.maxZ, box2.maxZ));

	}

	public static final AxisAlignedBoundingBoxXYZ intersect(
			AxisAlignedBoundingBoxXYZ box1, AxisAlignedBoundingBoxXYZ box2) {

		return new AxisAlignedBoundingBoxXYZ(
				Math.max(box1.minX, box2.minX),
				Math.max(box1.minY, box2.minY),
				Math.max(box1.minZ, box2.minZ),
				Math.min(box1.maxX, box2.maxX),
				Math.min(box1.maxY, box2.maxY),
				Math.min(box1.maxZ, box2.maxZ));

	}

}