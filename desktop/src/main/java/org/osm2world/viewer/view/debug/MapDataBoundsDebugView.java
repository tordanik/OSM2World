package org.osm2world.viewer.view.debug;

import java.awt.*;
import java.util.List;

import org.osm2world.math.VectorXZ;
import org.osm2world.output.jogl.JOGLOutput;

/**
 * shows the bounding boxes of map data
 */
public class MapDataBoundsDebugView extends DebugView {

	@Override
	public String getDescription() {
		return "shows the bounding boxes of map data";
	}

	@Override
	public boolean canBeUsed() {
		return map != null;
	}

	private static final Color DATA_BB_COLOR = Color.YELLOW;
	private static final Color FILE_BB_COLOR = Color.GREEN;

	@Override
	protected void fillTarget(JOGLOutput target) {

		List<VectorXZ> vs = map.getDataBoundary().polygonXZ().vertices();
		target.drawLineLoop(DATA_BB_COLOR, 1, VectorXZ.listXYZ(vs, 0));

		vs = map.getBoundary().polygonXZ().vertices();
		target.drawLineLoop(FILE_BB_COLOR, 1, VectorXZ.listXYZ(vs, 0));

	}

}
