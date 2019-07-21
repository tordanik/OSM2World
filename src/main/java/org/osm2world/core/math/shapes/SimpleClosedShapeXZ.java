package org.osm2world.core.math.shapes;

import java.util.Collection;

import org.osm2world.core.math.TriangleXZ;

/**
 * a closed shape, covering a non-zero area, that is not self-intersecting.
 * For this kind of shape, the vertices describe the area's boundary.
 */
public interface SimpleClosedShapeXZ extends ShapeXZ {

	/**
	 * returns a decomposition of the shape into triangles.
	 * For some shapes (e.g. circles), this may be an approximation.
	 */
	public Collection<TriangleXZ> getTriangulation();

}
