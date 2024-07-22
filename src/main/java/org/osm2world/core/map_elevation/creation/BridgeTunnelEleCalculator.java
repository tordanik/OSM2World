package org.osm2world.core.map_elevation.creation;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.world.modules.BridgeModule;
import org.osm2world.core.world.modules.TunnelModule;

/**
 * sets elevations to zero (or terrain elevation if available), except for bridges and tunnels
 */
public class BridgeTunnelEleCalculator extends TagEleCalculator {

	final double eleOffsetBridge;
	final double eleOffsetTunnel;

	private BridgeTunnelEleCalculator(double eleOffsetBridge, double eleOffsetTunnel) {
		this.eleOffsetBridge = eleOffsetBridge;
		this.eleOffsetTunnel = eleOffsetTunnel;
	}

	public BridgeTunnelEleCalculator() {
		this(0.1, 0.0);
	}

	@Override
	protected Double getEleForTags(TagSet tags, double terrainEle) {

		if (BridgeModule.isBridge(tags)) {
			return terrainEle + eleOffsetBridge;
		} else if (TunnelModule.isTunnel(tags)) {
			return terrainEle + eleOffsetTunnel;
		} else {
			return terrainEle;
		}

	}

}
