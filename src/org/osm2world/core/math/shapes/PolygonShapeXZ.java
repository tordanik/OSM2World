package org.osm2world.core.math.shapes;

import org.osm2world.core.math.PolygonXZ;
import org.osm2world.core.math.TriangleXZ;

/**
 * supertype for polygons, defined as closed 2d shapes with 3 or more vertices.
 * 
 * <p>{@link PolygonXZ} is the subclass that can represent any polygon.
 * Other subclasses are specialized to a subset, e.g. triangles in the case of {@link TriangleXZ}.
 */
public interface PolygonShapeXZ extends ShapeXZ {
	
	
	
}
