package org.osm2world.world.data;

import org.osm2world.map_data.data.MapNode;

public interface NodeWorldObject extends WorldObject {

	@Override
	public MapNode getPrimaryMapElement();

}
