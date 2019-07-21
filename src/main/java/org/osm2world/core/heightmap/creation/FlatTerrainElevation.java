package org.osm2world.core.heightmap.creation;

import org.osm2world.core.heightmap.data.AbstractCellularTerrainElevation;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXZ;

public class FlatTerrainElevation extends AbstractCellularTerrainElevation {

	public FlatTerrainElevation(AxisAlignedBoundingBoxXZ boundary,
			int numPointsX, int numPointsZ) {
		super(boundary, numPointsX, numPointsZ);
	}

	@Override
	protected Float getElevation(VectorXZ pos) {
		return 0f;
	}

}
