package org.osm2world.core.world.creation;

import org.osm2world.core.map_data.data.MapData;

public interface WorldModule {

	public void applyTo(MapData grid);
	
}
