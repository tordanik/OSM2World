package org.osm2world.viewer.view.debug;

import java.awt.*;
import java.util.List;

import org.osm2world.math.VectorXZ;
import org.osm2world.output.jogl.JOGLOutput;

/**
 * shows the bounding boxes of map data
 */
public class MapDataBoundsDebugView extends StaticDebugView {

	public MapDataBoundsDebugView() {
		super("Data bounds", "shows the bounding boxes of map data");
	}

	private static final Color DATA_BB_COLOR = Color.YELLOW;
	private static final Color FILE_BB_COLOR = Color.GREEN;

	@Override
	protected void fillOutput(JOGLOutput output) {

		List<VectorXZ> vs = scene.getBoundary().polygonXZ().vertices();
		output.drawLineLoop(DATA_BB_COLOR, 1, VectorXZ.listXYZ(vs, 0));

		vs = scene.getBoundary().polygonXZ().vertices();
		output.drawLineLoop(FILE_BB_COLOR, 1, VectorXZ.listXYZ(vs, 0));

	}

}
