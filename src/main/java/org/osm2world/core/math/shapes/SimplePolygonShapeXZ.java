package org.osm2world.core.math.shapes;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.Collection;

import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;

public interface SimplePolygonShapeXZ extends SimpleClosedShapeXZ, PolygonShapeXZ {

	/** returns the shape's outer ring. As this is already a shape without holes, it just returns the shape itself. */
	@Override
	default SimplePolygonShapeXZ getOuter() {
		return this;
	}

	/** returns the shape's holes. As this is a simple shape, the result will be empty. */
	@Override
	default Collection<? extends SimplePolygonShapeXZ> getHoles() {
		return emptyList();
	}

	/** returns true if the polygon contains a given position */
	@Override
	public default boolean contains(VectorXZ v) {
		return SimplePolygonXZ.contains(getVertexList(), v);
	}

	/** creates a new polygon by adding a shift vector to each vector of this */
	public default SimplePolygonShapeXZ shift(VectorXZ shiftVector) {
		return new SimplePolygonXZ(getVertexList().stream().map(shiftVector::add).collect(toList()));
	}

}
