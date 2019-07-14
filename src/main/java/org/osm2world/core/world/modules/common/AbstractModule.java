package org.osm2world.core.world.modules.common;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.world.creation.WorldModule;
import org.osm2world.core.world.data.WorldObject;

/**
 * skeleton implementation for {@link WorldModule}s.
 * 
 * Subclasses need to be able to create {@link WorldObject}s
 * for each {@link MapElement} in isolation.
 * This can make parallel application of the module possible.
 */
public abstract class AbstractModule extends ConfigurableWorldModule {
		
	@Override
	public final void applyTo(MapData grid) {
		
		for (MapNode node : grid.getMapNodes()) {
			applyToNode(node);
		}

		for (MapWaySegment segment : grid.getMapWaySegments()) {
			applyToWaySegment(segment);
		}

		for (MapArea area : grid.getMapAreas()) {
			applyToArea(area);
		}
		
	}

	/**
	 * create {@link WorldObject}s for a {@link MapElement}.
	 * Can be overwritten by subclasses.
	 * The default implementation does not create any objects.
	 */
	protected void applyToElement(MapElement element) {}
	
	/**
	 * create {@link WorldObject}s for a {@link MapNode}.
	 * Can be overwritten by subclasses.
	 * The default implementation calls {@link #applyToElement(MapElement)}.
	 */
	protected void applyToNode(MapNode node) {
		applyToElement(node);
	}
	
	/**
	 * create {@link WorldObject}s for a {@link MapWaySegment}.
	 * Can be overwritten by subclasses.
	 * The default implementation calls {@link #applyToElement(MapElement)}.
	 */
	protected void applyToWaySegment(MapWaySegment segment) {
		applyToElement(segment);
	}
	
	/**
	 * create {@link WorldObject}s for a {@link MapArea}.
	 * Can be overwritten by subclasses.
	 * The default implementation calls {@link #applyToElement(MapElement)}.
	 */
	protected void applyToArea(MapArea area) {
		applyToElement(area);
	}
	
}
