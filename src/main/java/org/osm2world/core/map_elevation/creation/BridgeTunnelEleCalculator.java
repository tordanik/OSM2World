package org.osm2world.core.map_elevation.creation;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.world.modules.BridgeModule;
import org.osm2world.core.world.modules.TunnelModule;

/**
 * sets elevations to zero, except for bridges and tunnels
 */
public class BridgeTunnelEleCalculator extends TagEleCalculator {

	final double eleBridge;
	final double eleTunnel;
	final Double eleTerrain;

	private BridgeTunnelEleCalculator(double eleBridge, double eleTunnel, Double eleTerrain) {
		this.eleBridge = eleBridge;
		this.eleTunnel = eleTunnel;
		this.eleTerrain = eleTerrain;
	}

	public BridgeTunnelEleCalculator() {
		this(0.1, 0.0, 0.0);
	}

	@Override
	protected Double getEleForTags(TagSet tags) {

		if (BridgeModule.isBridge(tags)) {
			return eleBridge;
		} else if (TunnelModule.isTunnel(tags)) {
			return eleTunnel;
		} else {
			return eleTerrain;
		}

	}

}
