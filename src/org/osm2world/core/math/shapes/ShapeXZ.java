package org.osm2world.core.math.shapes;

import java.util.List;

import org.osm2world.core.math.VectorXZ;

/**
 * two-dimensional, immutable shape. The shape is not required to be closed.
 * This has a variety of uses, including creating geometries by extrusion.
 * 
 * TODO create the hierarchy of interfaces and classes, for example:
 * 
 * Shape
 * - LineSegment
 * - SimpleShape (or "closed shape"?)
 * -- Ellipsis
 * --- Circle
 * -- PolygonShape
 * --- SimplePolygonXZ
 * --- Rectangle
 * ---- Square
 * --- Triangle
 * --- AxisAlignedBoundingBoxXZ
 */
public interface ShapeXZ {
	
	/**
	 * returns the shape's vertices.
	 * For closed shapes, the first and last vertex are identical.
	 *
	 * TODO support accuracy-dependent vertex collections (e.g. for circles)
	 * 
	 * @return list of vertices, not empty, not null
	 */
	public List<VectorXZ> getVertexList();
	
}
