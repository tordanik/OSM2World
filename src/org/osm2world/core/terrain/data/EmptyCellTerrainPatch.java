package org.osm2world.core.terrain.data;

import java.util.ArrayList;

import org.osm2world.core.heightmap.data.TerrainElevationCell;
import org.osm2world.core.heightmap.data.TerrainPoint;
import org.osm2world.core.math.TriangleXYZ;

/**
 * terrain patch for a special case, the empty terrain cell.
 * It can be handled more efficiently.
 * Except the performance advantages, it shouldn't affect program behavior.
 */
public class EmptyCellTerrainPatch extends TerrainPatch {

	TerrainElevationCell cell;
	
	public EmptyCellTerrainPatch(TerrainElevationCell cell) {
		this.cell = cell;
	}
	
	@Override
	public void build() {
		
		triangulation = new ArrayList<TriangleXYZ>(2);
		
		triangulation.add(triangleFromTerrainPoints(
				cell.getBottomLeft(),
				cell.getBottomRight(),
				cell.getTopLeft()));

		triangulation.add(triangleFromTerrainPoints(
				cell.getBottomRight(),
				cell.getTopRight(), 
				cell.getTopLeft()));
		
	}
	
	private TriangleXYZ triangleFromTerrainPoints(TerrainPoint p1, 
			TerrainPoint p2, TerrainPoint p3) {
		
		return new TriangleXYZ(p1.getPosXYZ(), p2.getPosXYZ(), p3.getPosXYZ());
		
	}

}
