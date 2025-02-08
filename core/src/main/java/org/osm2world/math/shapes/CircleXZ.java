package org.osm2world.math.shapes;

import static java.lang.Math.PI;
import static org.osm2world.math.shapes.SimplePolygonXZ.asSimplePolygon;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.math.VectorXZ;

public class CircleXZ implements SimpleClosedShapeXZ, RoundShapeXZ {

	private final VectorXZ center;
	private final double radius;

	public CircleXZ(VectorXZ center, double radius) {
		this.center = center;
		this.radius = radius;
	}

	public VectorXZ getCenter() {
		return center;
	}

	@Override
	public VectorXZ getCentroid() {
		return center;
	}

	public double getRadius() {
		return radius;
	}

	@Override
	public double getDiameter() {
		return 2 * radius;
	}

	@Override
	public double getArea() {
		return radius * radius * PI;
	}

	@Override
	public List<VectorXZ> vertices(int numPoints) {

		List<VectorXZ> result = new ArrayList<>(numPoints + 1);

		double angleInterval = 2 * PI / numPoints;

		for (int i = 0; i < numPoints; i++) {

			double angle = -i * angleInterval;
			double sin = Math.sin(angle);
			double cos = Math.cos(angle);

			result.add(center.add(new VectorXZ(radius * sin, radius * cos)));

		}

		result.add(result.get(0));

		return result;

	}

	@Override
	public List<TriangleXZ> getTriangulation() {

		List<VectorXZ> vertices = vertices();

		List<TriangleXZ> result = new ArrayList<>(vertices.size() - 1);

		for (int i = 0; i + 1 < vertices.size(); i++) {
			result.add(new TriangleXZ(center, vertices.get(i), vertices.get(i+1)));
		}

		return result;

	}

	@Override
	public List<VectorXZ> intersectionPositions(LineSegmentXZ lineSegment) {
		return asSimplePolygon(this).intersectionPositions(lineSegment);
	}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		return new AxisAlignedRectangleXZ(center.x - radius, center.z - radius, center.x + radius, center.z + radius);
	}

	@Override
	public boolean contains(VectorXZ v) {
		return v.distanceTo(center) <= radius;
	}

	@Override
	public CircleXZ rotatedCW(double angleRad) {
		return this;
	}

	@Override
	public CircleXZ shift(VectorXZ moveVector) {
		return new CircleXZ(center.add(moveVector), radius);
	}

	@Override
	public CircleXZ scale(double factor) {
		if (factor <= 0) throw new IllegalArgumentException("scale factor must be positive, was " + factor);
		return new CircleXZ(center, radius * factor);
	}

	@Override
	public CircleXZ mirrorX(double axisX) {
		return shift(getCenter().mirrorX(axisX).subtract(getCenter()));
	}

	@Override
	public boolean equals(Object other) {

		if (other instanceof CircleXZ) {
			return center.equals(((CircleXZ)other).center)
				&& radius == ((CircleXZ)other).radius;
		} else {
			return false;
		}

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((center == null) ? 0 : center.hashCode());
		long temp;
		temp = Double.doubleToLongBits(radius);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return "CircleXZ{center=" + center + ", radius=" + radius + "}";
	}

}
