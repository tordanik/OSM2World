package org.osm2world.viewer.view.debug;

import java.awt.Color;
import java.util.List;

import javax.media.opengl.GL2;

import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.jogl.JOGLTarget;

/**
 * shows the bounding boxes of map data
 */
public class MapDataBoundsDebugView extends DebugView {

	@Override
	public String getDescription() {
		return "shows the bounding boxes of map data";
	}
	
	private static final Color DATA_BB_COLOR = Color.YELLOW;
	private static final Color FILE_BB_COLOR = Color.GREEN;
		
	@Override
	protected void renderToImpl(GL2 gl, Camera camera, Projection projection) {
		
		JOGLTarget target = new JOGLTarget(gl, camera);

		List<VectorXZ> vs = map.getDataBoundary().polygonXZ().getVertexLoop();
		target.drawLineLoop(DATA_BB_COLOR, VectorXZ.listXYZ(vs, 0));

		vs = map.getBoundary().polygonXZ().getVertexLoop();
		target.drawLineLoop(FILE_BB_COLOR, VectorXZ.listXYZ(vs, 0));
		
	}
	
}
