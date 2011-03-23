package org.osm2world.viewer.view.debug;

import java.awt.Color;

import javax.media.opengl.GL;

import org.osm2world.core.heightmap.data.CellularTerrainElevation;
import org.osm2world.core.heightmap.data.TerrainPoint;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.target.jogl.RenderableToJOGL;

/**
 * shows the {@link CellularTerrainElevation}
 */
public class TerrainElevationGridDebugView extends DebugView implements RenderableToJOGL {

	private static final Color NODE_COLOR = Color.LIGHT_GRAY;
	private static final Color GRID_COLOR = Color.LIGHT_GRAY;
		
	@Override
	public void renderToImpl(GL gl, Camera camera) {

		JOGLTarget target = new JOGLTarget(gl, camera);

		TerrainPoint[][] grid = eleData.getTerrainPointGrid();
		for (int x = 0; x < grid.length; x++) {
			for (int z = 0; z < grid[x].length; z++) {
				if (x + 1 < grid.length) {
					target.drawLineStrip(GRID_COLOR, 1,
							grid[x][z].getPosXYZ(), grid[x+1][z].getPosXYZ());
				}
				if (z + 1 < grid[x].length) {
					target.drawLineStrip(GRID_COLOR, 1,
							grid[x][z].getPosXYZ(), grid[x][z+1].getPosXYZ());
				}
			}
		}
		
		for (TerrainPoint point : eleData.getTerrainPoints()) {
			target.drawPoints(NODE_COLOR, point.getPosXYZ());
		}
		
	}
	
}
