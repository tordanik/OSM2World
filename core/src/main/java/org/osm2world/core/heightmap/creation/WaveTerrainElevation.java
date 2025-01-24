package org.osm2world.core.heightmap.creation;

import org.osm2world.core.heightmap.data.AbstractCellularTerrainElevation;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.VectorXZ;

public class WaveTerrainElevation extends AbstractCellularTerrainElevation {

	public WaveTerrainElevation(AxisAlignedRectangleXZ boundary,
			int numPointsX, int numPointsZ) {
		super(boundary, numPointsX, numPointsZ);
	}

	@Override
	protected Float getElevation(VectorXZ pos) {
		return (float) Math.sin(pos.x) * 10;
	}

}
