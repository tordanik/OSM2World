package org.osm2world.core.world.data;

import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.datastructures.IntersectionTestObject;

/**
 * area covered by representations with this interface will not be
 * covered by terrain if the representation is on the ground
 * (according to {@link WorldObject#getGroundState()}).
 */
public interface TerrainBoundaryWorldObject extends WorldObjectWithOutline, IntersectionTestObject {

	/**
	 * returns the axis aligned bounding box that contains the entire object
	 */
	@Override
	public AxisAlignedBoundingBoxXZ boundingBox();

}
