package org.osm2world.core.world.data;

import static java.util.Collections.*;

import java.util.Collection;

import org.osm2world.core.math.shapes.PolygonShapeXZ;

/**
 * area covered by representations with this interface will not be
 * covered by terrain if the representation is on the ground
 * (according to {@link WorldObject#getGroundState()}).
 */
public interface TerrainBoundaryWorldObject extends WorldObject {

	/**
	 * returns a list of polygons defining an objects ground footprint in the xz plane.
	 * This area will not be covered by terrain (i.e. it will be a "hole" in the terrain surface).
	 *
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
