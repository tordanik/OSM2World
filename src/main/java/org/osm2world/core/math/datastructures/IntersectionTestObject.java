package org.osm2world.core.math.datastructures;

import org.osm2world.core.math.AxisAlignedRectangleXZ;

/**
 * object which can be inserted into data structures
 * that speed up intersection tests
 */
public interface IntersectionTestObject {

	public AxisAlignedRectangleXZ boundingBox();

}
