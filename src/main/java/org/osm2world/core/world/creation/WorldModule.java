package org.osm2world.core.world.creation;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.world.data.WorldObject;

public interface WorldModule {
	
	/**
	 * provides a {@link Configuration} that can be used to control aspects
	 * of a WorldModule's behavior.
	 * 
	 * This is guaranteed to be called before {@link #applyTo(MapData)},
	 * but not all parameters might be explicitly set in the configuration,
	 * so defaults need to be available.
	 */
	public void setConfiguration(Configuration config);
	
	/**
	 * adds {@link WorldObject}s to {@link MapElement}s
	 */
	public void applyTo(MapData mapData);
	
}
