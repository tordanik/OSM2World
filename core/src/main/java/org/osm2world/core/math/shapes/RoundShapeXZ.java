package org.osm2world.core.math.shapes;

import java.util.List;

import org.osm2world.core.math.VectorXZ;

/**
 * a shape that is at least partly round,
 * i.e. cannot be described with perfect accuracy by a finite number of vertices.
 * As such, methods like {@link #vertices()} produce an approximation.
 */
public interface RoundShapeXZ extends ShapeXZ {

	@Override
	public default List<VectorXZ> vertices() {
		// use a default number of points to approximate the shape with a polygon
		return vertices(36);
	}

	public List<VectorXZ> vertices(int numPoints);

}
