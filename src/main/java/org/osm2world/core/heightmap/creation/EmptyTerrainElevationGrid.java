package org.osm2world.core.heightmap.creation;

import org.osm2world.core.heightmap.data.AbstractCellularTerrainElevation;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.VectorXZ;

public class EmptyTerrainElevationGrid extends
		AbstractCellularTerrainElevation {

	public EmptyTerrainElevationGrid(AxisAlignedRectangleXZ bounds,
			int numPointsX, int numPointsZ) {
		super(bounds, numPointsX, numPointsZ);
	}

	@Override
	protected Float getElevation(VectorXZ pos) {
		return null;
	}

}
