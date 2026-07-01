package org.osm2world.map_elevation.creation;

import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.math.geo.LatLon;

public interface TerrainEleDataGridOrGridGroup extends TerrainEleData {

	List<TerrainEleDataGrid> grids();

	default @Nullable TerrainEleDataGrid gridAt(LatLon pos) {
		for (TerrainEleDataGrid grid : grids()) {
			if (grid.bounds().contains(pos)) {
				return grid;
			}
		}
		return null;
	}

}
