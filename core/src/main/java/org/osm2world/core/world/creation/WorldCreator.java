package org.osm2world.core.world.creation;

import static java.util.Collections.emptyMap;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.world.network.NetworkCalculator;

public class WorldCreator {

	private List<? extends WorldModule> modules;

	public WorldCreator(@Nullable Configuration config, WorldModule... modules) {
		this(config, Arrays.asList(modules));
	}

	public WorldCreator(@Nullable Configuration config, List<? extends WorldModule> modules) {

		this.modules = modules;

		if (config == null) {
			config = new MapConfiguration(emptyMap());
		}

		for (WorldModule module : modules) {
			module.setConfiguration(config);
		}

	}

	public void addRepresentationsTo(MapData mapData) {

		for (WorldModule module : modules) {
			module.applyTo(mapData);
		}

		NetworkCalculator.calculateNetworkInformationInMapData(mapData);

	}

}
