package org.osm2world.core.math.shapes;

import static java.util.Collections.emptyList;

import java.util.Collection;

import org.osm2world.core.math.TriangleXZ;

/**
 * a closed shape, covering a non-zero area, that is not self-intersecting and has no holes.
 */
public interface SimpleClosedShapeXZ extends ClosedShapeXZ {

	/**
	 * returns a decomposition of the shape into triangles.
	 * For some shapes (e.g. circles), this may be an approximation.
	 */
	public Collection<TriangleXZ> getTriangulation();

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

}
