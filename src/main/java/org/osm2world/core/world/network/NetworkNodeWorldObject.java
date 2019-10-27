package org.osm2world.core.world.network;

import static java.util.stream.Collectors.toSet;
import static org.osm2world.core.world.network.NetworkUtil.getConnectedNetworkSegments;

import java.util.Set;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.world.data.OutlineNodeWorldObject;
import org.osm2world.core.world.data.WorldObject;

/** parent type for junctions and connectors between {@link NetworkWaySegmentWorldObject}s, see there for more info */
public abstract class NetworkNodeWorldObject extends OutlineNodeWorldObject {

	protected NetworkNodeWorldObject(MapNode node) {
		super(node);
	}

	@Override
	public GroundState getGroundState() {

		Set<GroundState> connectedStates = getConnectedNetworkSegments(node, NetworkWaySegmentWorldObject.class, null)
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
