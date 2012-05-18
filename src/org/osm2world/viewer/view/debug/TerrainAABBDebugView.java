package org.osm2world.viewer.view.debug;

import java.awt.Color;

import javax.media.opengl.GL2;

import org.osm2world.core.heightmap.data.TerrainElevationCell;
import org.osm2world.core.math.PolygonXZ;
import org.osm2world.core.target.common.rendering.Camera;
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
	protected void renderToImpl(GL2 gl, Camera camera) {
		
		JOGLTarget target = new JOGLTarget(gl, camera);
		
		for (TerrainElevationCell cell : eleData.getCells()) {
			PolygonXZ polygon = cell.getAxisAlignedBoundingBoxXZ().polygonXZ();
			target.drawLineLoop(BB_COLOR, polygon.xyz(0).getVertices());
		}
		
	}
	
}
