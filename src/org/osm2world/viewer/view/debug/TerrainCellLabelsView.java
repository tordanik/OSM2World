package org.osm2world.viewer.view.debug;

import org.osm2world.core.target.jogl.JOGLTarget;

/** Shows each terrain cell's label */
public class TerrainCellLabelsView extends DebugView {

	@Override
	public String getDescription() {
		return "Shows each terrain cell's label";
	}
	
	@Override
	protected void fillTarget(JOGLTarget target) {

		//TODO this debug view is now useless
		
//		for (TerrainElevationCell cell : eleData.getCells()) {
//
//			//FIXME incorrect label placement
//
//			target.drawText(cell.toString(), cell.getPolygonXZ().getCenter(), Color.WHITE);
//
//		}
		
	}

}
