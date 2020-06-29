package org.osm2world.core.world.data;

import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.BoundedObject;
import org.osm2world.core.math.shapes.PolygonShapeXZ;

import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

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

	/**
	 * returns a list of polygons defining an objects ground footprint
	 * projected onto the xz plane
	 * @return collection of outline polygons, empty list if the world object doesn't cover any area
	 */
	public default Collection<PolygonShapeXZ> getTerrainBoundariesXZ() {
		if (getOutlinePolygonXZ() == null) {
			return emptyList();
		} else {
			return singletonList(getOutlinePolygonXZ());
		}
	}

}
