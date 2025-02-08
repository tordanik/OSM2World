package org.osm2world.world.creation;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.data.MapData;
import org.osm2world.map_data.data.MapElement;
import org.osm2world.world.data.WorldObject;

public interface WorldModule {

	/**
	 * provides an {@link O2WConfig} that can be used to control aspects
	 * of a WorldModule's behavior.
	 *
	 * This is guaranteed to be called before {@link #applyTo(MapData)},
	 * but not all parameters might be explicitly set in the configuration,
	 * so defaults need to be available.
	 */
	public void setConfiguration(O2WConfig config);

	/**
	 * adds {@link WorldObject}s to {@link MapElement}s
	 */
	public void applyTo(MapData mapData);

}
