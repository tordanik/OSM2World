package org.osm2world.world.creation;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.data.MapData;
import org.osm2world.world.network.NetworkCalculator;

public class WorldCreator {

	private List<? extends WorldModule> modules;

	public WorldCreator(@Nullable O2WConfig config, WorldModule... modules) {
		this(config, Arrays.asList(modules));
	}

	public WorldCreator(@Nullable O2WConfig config, List<? extends WorldModule> modules) {

		this.modules = modules;

		if (config == null) {
			config = new O2WConfig();
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
