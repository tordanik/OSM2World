package org.osm2world.core.math.shapes;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.SimplePolygonXZ.asSimplePolygon;

import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;

/**
 * a closed shape, covering a non-zero area, that is not self-intersecting and has no holes.
 */
public interface SimpleClosedShapeXZ extends ClosedShapeXZ {

	/** returns the shape's outer ring. As this is already a shape without holes, it just returns the shape itself. */
	@Override
	default SimpleClosedShapeXZ getOuter() {
		return this;
	}

	/** returns the shape's holes. As this is a simple shape, the result will be empty. */
	@Override
	default Collection<? extends SimpleClosedShapeXZ> getHoles() {
		return emptyList();
	}

	/** returns true if the shape has clockwise orientation */
	default boolean isClockwise() {
		return asSimplePolygon(this).isClockwise();
	}

	/** returns the centroid (or "barycenter") of this shape */
	public default VectorXZ getCentroid() {
		return asSimplePolygon(this).getCentroid();
	}

	/** returns the largest distance between any pair of vertices of this shape */
	public double getDiameter();

	/**
	 * returns a scaled version of this shape.
	 *
	 * @param factor  the scale factor, must be greater than 0. Values greater than 1 grow the shape.
	 * @param center  the center of the scale operation
	 */
	default	SimpleClosedShapeXZ scale(VectorXZ center, double factor) {
		if (factor <= 0) throw new IllegalArgumentException("scale factor must be positive, was " + factor);
		return scale(center, factor, factor);
	}

	/**
	 * returns a scaled version of this shape, like {@link #scale(VectorXZ, double)},
	 * and uses this shape's {@link #getCentroid()} as the center of the scale operation.
	 */
	default	SimpleClosedShapeXZ scale(double factor) {
		return scale(this.getCentroid(), factor);
	}

	/**
	 * returns a scaled version of this shape.
	 *
	 * @param factorX  the scale factor in x dimension, must be greater than 0. Values greater than 1 grow the shape.
	 * @param factorZ  the scale factor in z dimension, must be greater than 0. Values greater than 1 grow the shape.
	 * @param center  the center of the scale operation
	 */
	default	SimpleClosedShapeXZ scale(VectorXZ center, double factorX, double factorZ) {
		if (factorX <= 0) throw new IllegalArgumentException("x scale factor must be positive, was " + factorX);
		if (factorZ <= 0) throw new IllegalArgumentException("z scale factor must be positive, was " + factorZ);
		if (factorX == 1 && factorZ == 1) return this;
		List<VectorXZ> vertices = vertices().stream()
				.map(v -> v.subtract(center))
				.map(v -> new VectorXZ(v.x * factorX, v.z * factorZ))
				.map(v -> v.add(center))
				.collect(toList());
		return new SimplePolygonXZ(vertices);
	}

	/**
	 * returns a scaled version of this shape, like {@link #scale(VectorXZ, double, double)},
	 * and uses this shape's {@link #getCentroid()} as the center of the scale operation.
	 */
	default	SimpleClosedShapeXZ scale(double factorX, double factorZ) {
		return scale(this.getCentroid(), factorX, factorZ);
	}

}
