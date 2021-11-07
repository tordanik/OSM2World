package org.osm2world.core.world.data;

import org.osm2world.core.map_data.data.MapArea;

public interface AreaWorldObject extends LegacyWorldObject {

	@Override
	public MapArea getPrimaryMapElement();

}
