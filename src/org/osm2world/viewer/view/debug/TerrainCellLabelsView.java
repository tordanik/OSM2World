package org.osm2world.viewer.view.debug;

import java.awt.Color;

import javax.media.opengl.GL;

import org.osm2world.core.heightmap.data.TerrainElevationCell;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.jogl.JOGLTarget;

/** Shows each terrain cell's label */
public class TerrainCellLabelsView extends DebugView {

	@Override
	public String getDescription() {
		return "Shows each terrain cell's label";
	}
	
	@Override
	protected void renderToImpl(GL gl, Camera camera, Projection projection) {
		
		JOGLTarget target = new JOGLTarget(gl, camera);

		for (TerrainElevationCell cell : eleData.getCells()) {
			
			//FIXME incorrect label placement
			
			target.drawText(cell.toString(), cell.getPolygonXZ().getCenter(), Color.WHITE);
			
		}
		
	}

}
