package org.osm2world.core.math.shapes;

import static java.lang.Math.PI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;

public class CircleXZ implements SimpleClosedShapeXZ {

	/** default number of points used to approximate the circle with a polygon */
	private static final int NUM_POINTS = 36;

	private final VectorXZ center;
	private final double radius;

	public CircleXZ(VectorXZ center, double radius) {
		this.center = center;
		this.radius = radius;
	}

	public VectorXZ getCenter() {
		return center;
	}

	public double getRadius() {
		return radius;
	}

	@Override
	public double getArea() {
		return radius * radius * PI;
	}

	public List<VectorXZ> getVertices(int numPoints) {

		List<VectorXZ> result = new ArrayList<VectorXZ>(numPoints + 1);

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
	public List<VectorXZ> vertices() {
		return getVertices(NUM_POINTS);
	}

	@Override
	public Collection<TriangleXZ> getTriangulation() {

		List<VectorXZ> vertices = vertices();

		List<TriangleXZ> result = new ArrayList<>(vertices.size() - 1);

		for (int i = 0; i + 1 < vertices.size(); i++) {
			result.add(new TriangleXZ(center, vertices.get(i), vertices.get(i+1)));
		}

		return result;

	}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {
		return new AxisAlignedRectangleXZ(center.x - radius, center.z - radius, center.x + radius, center.z + radius);
	}

	@Override
	public ShapeXZ rotatedCW(double angleRad) {
		return this;
	}

	@Override
	public ShapeXZ shift(VectorXZ moveVector) {
		return new CircleXZ(center.add(moveVector), radius);
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
