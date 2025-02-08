package org.osm2world.world.modules.common;

import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.MapData;
import org.osm2world.map_data.data.MapElement;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.MapWay;
import org.osm2world.map_data.data.MapWaySegment;
import org.osm2world.world.creation.WorldModule;
import org.osm2world.world.data.WorldObject;

/**
 * skeleton implementation for {@link WorldModule}s.
 *
 * Subclasses need to be able to create {@link WorldObject}s
 * for each {@link MapElement} in isolation.
 * This can make parallel application of the module possible.
 */
public abstract class AbstractModule extends ConfigurableWorldModule {

	@Override
	public final void applyTo(MapData mapData) {

		for (MapNode node : mapData.getMapNodes()) {
			applyToNode(node);
		}

		for (MapWay way : mapData.getMapWays()) {
			applyToWay(way);
		}

		for (MapWaySegment waySegment : mapData.getMapWaySegments()) {
			applyToWaySegment(waySegment);
		}

		for (MapArea area : mapData.getMapAreas()) {
			applyToArea(area);
		}

	}

	/**
	 * create {@link WorldObject}s for a {@link MapElement}.
	 * Can be overwritten by subclasses.
	 * The default implementation does not create any objects.
	 */
	protected void applyToElement(@SuppressWarnings("unused") MapElement element) {}

	/**
	 * create {@link WorldObject}s for a {@link MapNode}.
	 * Can be overwritten by subclasses.
	 * The default implementation calls {@link #applyToElement(MapElement)}.
	 */
	protected void applyToNode(MapNode node) {
		applyToElement(node);
	}

	/**
	 * create {@link WorldObject}s for a {@link MapWay}.
	 * Can be overwritten by subclasses.
	 * The default implementation does nothing. Note that {@link #applyToWaySegment(MapWaySegment)} is called
	 * for each of the way's segments regardless of whether or not this method is overwritten.
	 */
	protected void applyToWay(@SuppressWarnings("unused") MapWay way) {}

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
