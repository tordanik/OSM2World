package org.osm2world.core.world.data;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.Collection;

import org.osm2world.core.math.shapes.PolygonShapeXZ;

/**
 * a {@link WorldObject} with a (typically) non-empty {@link #getTerrainBoundariesXZ()}
 */
public interface TerrainBoundaryWorldObject extends WorldObject {

	@Override
	public default Collection<PolygonShapeXZ> getTerrainBoundariesXZ() {
		if (getOutlinePolygonXZ() == null) {
			return emptyList();
		} else {
			return singletonList(getOutlinePolygonXZ());
		}
	}

}
