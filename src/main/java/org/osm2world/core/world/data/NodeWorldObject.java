package org.osm2world.core.world.data;

import org.osm2world.core.map_data.data.MapNode;

public interface NodeWorldObject extends LegacyWorldObject {

	@Override
	public MapNode getPrimaryMapElement();

}
