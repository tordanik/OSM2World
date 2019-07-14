package org.osm2world.core.world.data;

import org.osm2world.core.map_data.data.MapArea;

public interface AreaWorldObject extends WorldObject {
	
	@Override
	public MapArea getPrimaryMapElement();
	
}
