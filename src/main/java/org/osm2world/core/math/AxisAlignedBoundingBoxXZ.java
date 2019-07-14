package org.osm2world.core.math;

import static java.lang.Math.*;
import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.datastructures.IntersectionTestObject;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.math.shapes.SimpleClosedShapeXZ;

/**
 * immutable representation of an axis-aligned bounding box
 * with x and z dimensions
 * 
 * TODO: rename to AxisAlignedRectangleXZ?
 */
public class AxisAlignedBoundingBoxXZ implements Cloneable, PolygonShapeXZ, SimpleClosedShapeXZ {

	public final double minX, minZ, maxX, maxZ;

	public AxisAlignedBoundingBoxXZ(double minX, double minZ,
			double maxX, double maxZ) {
		this.minX = minX;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxZ = maxZ;
	}
	
	/**
	 * @param boundedPoints  must contain at least one point
	 */
	public AxisAlignedBoundingBoxXZ(Collection<? extends Vector3D> boundedPoints) {
		
		assert (!boundedPoints.isEmpty());
		
		double minX = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
		
		for (Vector3D p : boundedPoints) {
			minX = min(minX, p.getX()); minZ = min(minZ, p.getZ());
			maxX = max(maxX, p.getX()); maxZ = max(maxZ, p.getZ());
		}
		
		this.minX = minX; this.minZ = minZ;
		this.maxX = maxX; this.maxZ = maxZ;
		
	}
	
	public double sizeX() { return maxX - minX; }
	public double sizeZ() { return maxZ - minZ; }

	/**
	 * returns the area covered by this bounding box
	 */
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
	
	private SimplePolygonXZ polygonXZ;
	
	public SimplePolygonXZ polygonXZ() {
		
		if (polygonXZ == null) {
			polygonXZ = new SimplePolygonXZ(getVertexList());
		}
		
		return polygonXZ;
		
	}
	
	public VectorXZ bottomLeft() {
		return polygonXZ().getVertexCollection().get(0);
	}
	
	public VectorXZ bottomRight() {
		return polygonXZ().getVertexCollection().get(1);
	}
	
	public VectorXZ topRight() {
		return polygonXZ().getVertexCollection().get(2);
	}
	
	public VectorXZ topLeft() {
		return polygonXZ().getVertexCollection().get(3);
	}
	
	@Override
	public Collection<TriangleXZ> getTriangulation() {
		return asList(
				new TriangleXZ(topLeft(), bottomRight(), topRight()),
				new TriangleXZ(topLeft(), bottomLeft(), bottomRight()));
	}
	
	/**
	 * returns a bounding box that is a bit larger than this one
	 */
	public AxisAlignedBoundingBoxXZ pad(double paddingSize) {
		return new AxisAlignedBoundingBoxXZ(
				minX - paddingSize,
				minZ - paddingSize,
				maxX + paddingSize,
				maxZ + paddingSize);
	}

	public boolean overlaps(AxisAlignedBoundingBoxXZ otherBox) {
		return !(maxX <= otherBox.minX
				|| minX >= otherBox.maxX
				|| maxZ <= otherBox.minZ
				|| minZ >= otherBox.maxZ);
	}

	public boolean contains(AxisAlignedBoundingBoxXZ otherBox) {
		return minX <= otherBox.minX
				&& minZ <= otherBox.minZ
				&& maxX >= otherBox.maxX
				&& maxZ >= otherBox.maxZ;
	}

	public boolean contains(IntersectionTestObject object) {
		return polygonXZ().contains(
				object.getAxisAlignedBoundingBoxXZ().polygonXZ());
	}

	public boolean contains(VectorXZ v) {
		return v.x >= minX && v.x <= maxX && v.z >= minZ && v.z <= maxZ;
	}
	
	public static final AxisAlignedBoundingBoxXZ union(
			AxisAlignedBoundingBoxXZ box1, AxisAlignedBoundingBoxXZ box2) {
		
		return new AxisAlignedBoundingBoxXZ(
				Math.min(box1.minX, box2.minX),
				Math.min(box1.minZ, box2.minZ),
				Math.max(box1.maxX, box2.maxX),
				Math.max(box1.maxZ, box2.maxZ));
		
	}
	
	@Override
	public AxisAlignedBoundingBoxXZ clone() {
		try {
			return (AxisAlignedBoundingBoxXZ) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError("unexpected super.clone behavior");
		}
	}
	
}
