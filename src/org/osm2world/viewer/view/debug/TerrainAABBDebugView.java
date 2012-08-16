package org.osm2world.viewer.view.debug;

import java.awt.Color;

import org.osm2world.core.heightmap.data.TerrainElevationCell;
import org.osm2world.core.math.PolygonXZ;
import org.osm2world.core.target.jogl.JOGLTarget;

/**
 * shows the axis-aligned bounding boxes of the terrain cells
 */
public class TerrainAABBDebugView extends DebugView {
	
	@Override
	public String getDescription() {
		return "shows the axis-aligned bounding boxes of the terrain cells";
	}
	
	private static final Color BB_COLOR = Color.WHITE;
	
	@Override
	protected void fillTarget(JOGLTarget target) {
		
		for (TerrainElevationCell cell : eleData.getCells()) {
			PolygonXZ polygon = cell.getAxisAlignedBoundingBoxXZ().polygonXZ();
			target.drawLineLoop(BB_COLOR, 1, polygon.xyz(0).getVertices());
		}
		
	}
	
}
