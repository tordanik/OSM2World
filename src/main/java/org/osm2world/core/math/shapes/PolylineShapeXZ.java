package org.osm2world.core.math.shapes;

import java.util.List;

import org.osm2world.core.math.LineSegmentXZ;

/**
 * a polyline (aka linestring) with at least two points.
 * This is an open shape, i.e. it does not represent a non-zero area.
 */
public interface PolylineShapeXZ extends ShapeXZ {

	/**
	 * returns the length of the entire polyline
	 */
	public double getLength();

	/**
	 * returns the ordered list of segments between the vertices
	 */
	List<LineSegmentXZ> getSegments();

}
