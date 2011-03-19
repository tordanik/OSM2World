package org.osm2world.core.world.creation;

import java.util.Arrays;
import java.util.Collection;

import org.osm2world.core.map_data.data.MapData;

public class WorldCreator {

	private Collection<WorldModule> modules;
		
	public WorldCreator(WorldModule... modules) {
		this.modules = Arrays.asList(modules);
	}
	
	public WorldCreator(Collection<WorldModule> modules) {
		this.modules = modules;
	}
	
	public void addRepresentationsTo(MapData grid) {
		
		for (WorldModule module : modules) {
			module.applyTo(grid);
		}
		
		NetworkCalculator.calculateNetworkInformationInGrid(grid);
		
	}
	
}
