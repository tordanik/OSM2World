package org.osm2world.viewer.view.debug;

import java.awt.Color;

import org.osm2world.core.heightmap.data.CellularTerrainElevation;
import org.osm2world.core.target.jogl.JOGLTarget;

/**
 * shows the {@link CellularTerrainElevation}
 */
public class TerrainElevationGridDebugView extends DebugView {

	private static final Color NODE_COLOR = Color.LIGHT_GRAY;
	private static final Color GRID_COLOR = Color.LIGHT_GRAY;
		
	@Override
	public void fillTarget(JOGLTarget target) {

		//TODO this debug view is now useless
		
//		TerrainPoint[][] grid = eleData.getTerrainPointGrid();
//		for (int x = 0; x < grid.length; x++) {
//			for (int z = 0; z < grid[x].length; z++) {
//				if (x + 1 < grid.length) {
//					target.drawLineStrip(GRID_COLOR, 1,
//							grid[x][z].getPosXYZ(), grid[x+1][z].getPosXYZ());
//				}
//				if (z + 1 < grid[x].length) {
//					target.drawLineStrip(GRID_COLOR, 1,
//							grid[x][z].getPosXYZ(), grid[x][z+1].getPosXYZ());
//				}
//			}
//		}
//
//		for (TerrainPoint point : eleData.getTerrainPoints()) {
//			target.drawPoints(NODE_COLOR, point.getPosXYZ());
//		}
		
	}
	
}
