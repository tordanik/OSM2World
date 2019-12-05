package org.osm2world.core.world.network;

import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.world.data.OutlineNodeWorldObject;
import org.osm2world.core.world.data.WorldObject;

/**
 * parent type for junctions and connectors between {@link NetworkWaySegmentWorldObject}s, see there for more info
 * @param <S>  the type of {@link NetworkWaySegmentWorldObject} connected by this network node
 */
public abstract class NetworkNodeWorldObject<S extends NetworkWaySegmentWorldObject> extends OutlineNodeWorldObject {

	private final Class<S> segmentType;

	protected NetworkNodeWorldObject(MapNode node, Class<S> segmentType) {
		super(node);
		this.segmentType = segmentType;
	}

	/** version of {@link #getConnectedNetworkSegments(Predicate)} without a filter */
	public List<S> getConnectedNetworkSegments() {
		return NetworkUtil.getConnectedNetworkSegments(node, segmentType, null);
	}

	/** @see NetworkUtil#getConnectedNetworkSegments(MapNode, Class, Predicate) */
	public List<S> getConnectedNetworkSegments(Predicate<? super S> filter) {
		return NetworkUtil.getConnectedNetworkSegments(node, segmentType, filter);
	}

	@Override
	public GroundState getGroundState() {

		Set<GroundState> connectedStates = getConnectedNetworkSegments()
			.stream()
			.map(WorldObject::getGroundState)
			.collect(toSet());

		if (connectedStates.size() == 1) {
			return connectedStates.iterator().next();
		} else {
			// transition between different states, must be ground level
			return GroundState.ON;
		}

	}

}
