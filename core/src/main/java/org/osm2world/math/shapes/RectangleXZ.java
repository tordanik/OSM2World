package org.osm2world.math.shapes;

import static java.lang.Math.abs;

import java.util.List;

import javax.annotation.Nonnull;

import org.osm2world.math.Angle;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.GeometryUtil;

/**
 * immutable representation of an axis-aligned rectangle with x and z dimensions.
 * Often used to represent bounding boxes.
 * For axis-aligned bounding boxes, use {@link AxisAlignedRectangleXZ} instead.
 */
public record RectangleXZ(VectorXZ v0, VectorXZ v1, VectorXZ v2, VectorXZ v3) implements SimplePolygonShapeXZ {

	public RectangleXZ {

		Angle[] sideAngles = new Angle[] {
			Angle.ofRadians(v0.angleTo(v1)),
			Angle.ofRadians(v1.angleTo(v2)),
			Angle.ofRadians(v2.angleTo(v3)),
			Angle.ofRadians(v3.angleTo(v0))
		};

		for (int i = 0; i < 3; i++) {
			if (abs(Angle.radiansBetween(sideAngles[i], sideAngles[i + 1]) - Math.PI / 2) > 0.01) {
				throw new IllegalArgumentException("RectangleXZ requires right angles");
			}
		}

	}

	@Override
	@Nonnull
	public String toString() {
		return verticesNoDup().toString();
	}

	@Override
	public VectorXZ getVertex(int index) {
		return switch (index) {
		case 0 -> v0;
		case 1 -> v1;
		case 2 -> v2;
		case 3 -> v3;
		default -> throw new IndexOutOfBoundsException();
		};
	}

	@Override
	public SimplePolygonXZ polygonXZ() {
		return new SimplePolygonXZ(vertices());
	}

	@Override
	public List<TriangleXZ> getTriangulation() {
		return List.of(new TriangleXZ(v0, v1, v3), new TriangleXZ(v1, v2, v3));
	}

	@Override
	public List<VectorXZ> vertices() {
		return List.of(v0, v1, v2, v3, v0);
	}

	@Override
	public List<VectorXZ> verticesNoDup() {
		return List.of(v0, v1, v2, v3);
	}

	@Override
	public VectorXZ getCentroid() {
		return v0.add(v2).mult(0.5);
	}

	@Override
	public double getArea() {
		return v0.distanceTo(v1) * v1.distanceTo(v2);
	}

	@Override
	public boolean isClockwise() {
		return GeometryUtil.isRightOf(v2, v0, v1);
	}

	public RectangleXZ reverse() {
		return new RectangleXZ(v3, v2, v1, v0);
	}

	@Override
	public RectangleXZ shift(VectorXZ moveVector) {
		return new RectangleXZ(
				v0.add(moveVector),
				v1.add(moveVector),
				v2.add(moveVector),
				v3.add(moveVector) );
	}

}
