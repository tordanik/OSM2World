package org.osm2world.world.modules.building;

import static org.osm2world.world.connectors.GroundState.ON;

import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.MapWaySegment;
import org.osm2world.world.connectors.GroundState;
import org.osm2world.world.modules.BridgeModule;
import org.osm2world.world.modules.TunnelModule;

public record BuildingEntrance(MapNode node, double relativeEle) {

	/** guesses the ground state for this entrance's connector based on other features connected to it */
	public GroundState groundState() {

		boolean bridge = node.getAdjacentAreas().stream()
				.anyMatch(a -> a.getTags().contains("man_made", "bridge"));
		boolean tunnel = false;

		for (MapWaySegment segment : node.getConnectedWaySegments()) {
			bridge |= BridgeModule.isBridge(segment.getTags());
			tunnel |= TunnelModule.isTunnel(segment.getTags());
		}

		if (bridge) {
			return GroundState.ABOVE;
		} else if (tunnel) {
			return GroundState.BELOW;
		} else {
			return ON;
		}

	}

}
