package org.osm2world.core.map_elevation.creation;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.world.modules.BridgeModule;
import org.osm2world.core.world.modules.TunnelModule;

/**
 * sets elevations to zero, except for bridges and tunnels
 */
public class BridgeTunnelElevationCalculator extends TagElevationCalculator {

	final double eleBridge;
	final double eleTunnel;

	private BridgeTunnelElevationCalculator(double eleBridge, double eleTunnel) {
		super(0.0, false);
		this.eleBridge = eleBridge;
		this.eleTunnel = eleTunnel;
	}

	public BridgeTunnelElevationCalculator() {
		this(1, 0);
	}

	@Override
	protected Double getEleForTags(TagSet tags) {

		if (BridgeModule.isBridge(tags)) {
			return eleBridge;
		} else if (TunnelModule.isTunnel(tags)) {
			return eleTunnel;
		} else {
			return null;
		}

	}

}
