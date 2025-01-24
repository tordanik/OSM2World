package org.osm2world.core.math.shapes;

import static java.lang.Math.PI;
import static java.util.Collections.reverse;
import static org.osm2world.core.math.SimplePolygonXZ.asSimplePolygon;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.Angle;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;


public class CircularSectorXZ implements SimpleClosedShapeXZ, RoundShapeXZ {

	private final VectorXZ center;
	private final double radius;
	private final Angle startAngle;
	private final Angle endAngle;

	public CircularSectorXZ(VectorXZ center, double radius, Angle startAngle, Angle endAngle) {
		this.center = center;
		this.radius = radius;
		this.startAngle = startAngle;
		this.endAngle = endAngle;
	}

	@Override
	public List<VectorXZ> vertices(int numPoints) {

		List<VectorXZ> result = new ArrayList<>(numPoints + 1);

		Angle angleInterval = endAngle.minus(startAngle).div(numPoints - 1);

		for (int i = 0; i < numPoints; i++) {
			Angle angle = startAngle.plus(angleInterval.times(i));
			result.add(center.add(VectorXZ.fromAngle(angle).mult(radius)));
		}

		reverse(result); // make counterclockwise
		result.add(result.get(0));

		return result;

	}

	@Override
	public double getDiameter() {
		return asSimplePolygon(this).getDiameter();
	}

	@Override
	public double getArea() {
		double circleArea = radius * radius * PI;
		return circleArea * endAngle.minus(startAngle).radians / (2 * PI);
	}

	@Override
	public ShapeXZ shift(VectorXZ moveVector) {
		return new CircularSectorXZ(center.add(moveVector), radius, startAngle, endAngle);
	}

	@Override
	public CircularSectorXZ rotatedCW(double angleRad) {
		Angle rotAngle = Angle.ofRadians(angleRad);
		return new CircularSectorXZ(center, radius, startAngle.plus(rotAngle), endAngle.plus(rotAngle));
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
		// TODO Auto-generated method stub
		return null;
	}

}
