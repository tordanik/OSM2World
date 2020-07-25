package org.osm2world.core.math;

import static java.lang.Math.*;
import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.shapes.SimplePolygonShapeXZ;

/**
 * immutable representation of an axis-aligned rectangle with x and z dimensions.
 * Often used to represent bounding boxes.
 */
public class AxisAlignedRectangleXZ implements SimplePolygonShapeXZ {

	public final double minX, minZ, maxX, maxZ;

	public AxisAlignedRectangleXZ(double minX, double minZ, double maxX, double maxZ) {
		this.minX = minX;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxZ = maxZ;
	}

	public AxisAlignedRectangleXZ(VectorXZ center, double sizeX, double sizeZ) {
		this.minX = center.x - sizeX / 2;
		this.minZ = center.z - sizeZ / 2;
		this.maxX = center.x + sizeX / 2;
		this.maxZ = center.z + sizeZ / 2;
	}

	/**
	 * @param boundedPoints  must contain at least one point
	 */
	public static AxisAlignedRectangleXZ bbox(Collection<? extends Vector3D> boundedPoints) {

		assert (!boundedPoints.isEmpty());

		double minX = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

		for (Vector3D p : boundedPoints) {
			minX = min(minX, p.getX()); minZ = min(minZ, p.getZ());
			maxX = max(maxX, p.getX()); maxZ = max(maxZ, p.getZ());
		}

		return new AxisAlignedRectangleXZ(minX, minZ, maxX, maxZ);

	}

	/**
	 * @param boundedObjects  must contain at least one object
	 */
	public static AxisAlignedRectangleXZ bboxUnion(Collection<? extends BoundedObject> boundedObjects) {

		assert (!boundedObjects.isEmpty());

		double minX = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

		for (BoundedObject boundedObject : boundedObjects) {
			AxisAlignedRectangleXZ bbox = boundedObject.boundingBox();
			minX = min(minX, bbox.minX); minZ = min(minZ, bbox.minZ);
			maxX = max(maxX, bbox.maxX); maxZ = max(maxZ, bbox.maxZ);
		}

		return new AxisAlignedRectangleXZ(minX, minZ, maxX, maxZ);

	}

	public double sizeX() { return maxX - minX; }
	public double sizeZ() { return maxZ - minZ; }

	/** returns the area covered by this rectangle */
	public double area() {
		return sizeX() * sizeZ();
	}

	@Override
	public List<VectorXZ> getVertexList() {

		VectorXZ v0 = new VectorXZ(minX, minZ);
		VectorXZ v1 = new VectorXZ(maxX, minZ);
		VectorXZ v2 = new VectorXZ(maxX, maxZ);
		VectorXZ v3 = new VectorXZ(minX, maxZ);

		return asList(v0, v1, v2, v3, v0);

	}

	public VectorXZ center() {
		return new VectorXZ(minX + sizeX()/2, minZ + sizeZ()/2);
	}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		return this;
	}

	@Override
	public SimplePolygonXZ minimumRotatedBoundingBox() {
		return polygonXZ();
	}

	@Override
	public SimplePolygonShapeXZ convexHull() {
		return this;
	}

	private SimplePolygonXZ polygonXZ;

	public SimplePolygonXZ polygonXZ() {

		if (polygonXZ == null) {
			polygonXZ = new SimplePolygonXZ(getVertexList());
		}

		return polygonXZ;

	}

	public VectorXZ bottomLeft() {
		return polygonXZ().getVertex(0);
	}

	public VectorXZ bottomRight() {
		return polygonXZ().getVertex(1);
	}

	public VectorXZ topRight() {
		return polygonXZ().getVertex(2);
	}

	public VectorXZ topLeft() {
		return polygonXZ().getVertex(3);
	}

	@Override
	public Collection<TriangleXZ> getTriangulation() {
		return asList(
				new TriangleXZ(topLeft(), bottomRight(), topRight()),
				new TriangleXZ(topLeft(), bottomLeft(), bottomRight()));
	}

	/** returns a rectangle that is a bit larger than this one */
	public AxisAlignedRectangleXZ pad(double paddingSize) {
		return new AxisAlignedRectangleXZ(
				minX - paddingSize,
				minZ - paddingSize,
				maxX + paddingSize,
				maxZ + paddingSize);
	}

	public boolean overlaps(AxisAlignedRectangleXZ otherBox) {
		return !(maxX <= otherBox.minX
				|| minX >= otherBox.maxX
				|| maxZ <= otherBox.minZ
				|| minZ >= otherBox.maxZ);
	}

	public boolean contains(AxisAlignedRectangleXZ otherBox) {
		return minX <= otherBox.minX
				&& minZ <= otherBox.minZ
				&& maxX >= otherBox.maxX
				&& maxZ >= otherBox.maxZ;
	}

	public boolean contains(VectorXZ v) {
		return v.x >= minX && v.x <= maxX && v.z >= minZ && v.z <= maxZ;
	}

	public static final AxisAlignedRectangleXZ union(
			AxisAlignedRectangleXZ box1, AxisAlignedRectangleXZ box2) {

		return new AxisAlignedRectangleXZ(
				Math.min(box1.minX, box2.minX),
				Math.min(box1.minZ, box2.minZ),
				Math.max(box1.maxX, box2.maxX),
				Math.max(box1.maxZ, box2.maxZ));

	}

}
