package org.osm2world.core.math;

import org.osm2world.core.math.shapes.AxisAlignedRectangleXZ;

/**
 * object which has an extent in the XZ plane that can be enclosed with a rectangle
 */
public interface BoundedObject {

	/** returns the minimum axis-aligned bounding rectangle of this object */
	public AxisAlignedRectangleXZ boundingBox();

}
