package org.osm2world.core.map_elevation.creation;

import javax.annotation.Nonnull;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.util.FaultTolerantIterationUtil;
import org.osm2world.core.world.data.WorldObject;

/**
 * assigns an elevation of 0 to everything.
 * Useful for certain use cases, e.g. fast creation of tiled pseudo-3D tiles.
 */
public class ZeroElevationCalculator implements ElevationCalculator {

	@Override
	public void calculateElevations(@Nonnull MapData mapData) {

		FaultTolerantIterationUtil.forEach(mapData.getWorldObjects(), (WorldObject worldObject) -> {
			for (EleConnector conn : worldObject.getEleConnectors()) {
				conn.setPosXYZ(conn.pos.xyz(0));
			}
		});

	}

}
