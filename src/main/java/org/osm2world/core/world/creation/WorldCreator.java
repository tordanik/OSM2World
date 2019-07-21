package org.osm2world.core.world.creation;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.map_data.data.MapData;

public class WorldCreator {

	private List<? extends WorldModule> modules;

	public WorldCreator(Configuration config, WorldModule... modules) {
		this(config, Arrays.asList(modules));
	}

	public WorldCreator(Configuration config, List<? extends WorldModule> modules) {
		this.modules = modules;
		for (WorldModule module : modules) {
			module.setConfiguration(config);
		}
	}

	public void addRepresentationsTo(MapData mapData) {

		for (WorldModule module : modules) {
			module.applyTo(mapData);
		}

		NetworkCalculator.calculateNetworkInformationInGrid(mapData);

	}

}
