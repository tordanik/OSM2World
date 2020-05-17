package org.osm2world.core.world.data;

import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.BoundedObject;

/**
 * area covered by representations with this interface will not be
 * covered by terrain if the representation is on the ground
 * (according to {@link WorldObject#getGroundState()}).
 */
public interface TerrainBoundaryWorldObject extends WorldObjectWithOutline, BoundedObject {

	/**
	 * returns the axis aligned bounding box that contains the entire object
	 */
	@Override
	public AxisAlignedRectangleXZ boundingBox();

}
