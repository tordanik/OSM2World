package org.osm2world.core.math.datastructures;

import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;

/**
 * object which can be inserted into data structures
 * that speed up intersection tests
 */
public interface IntersectionTestObject {

	public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ();

}
