package org.osm2world.viewer.view.debug;

import java.awt.Color;
import java.util.List;

import javax.media.opengl.GL;

import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.jogl.JOGLTarget;

/**
 * shows the bounding box of map data
 */
public class MapDataBoundsDebugView extends DebugView {

	@Override
	public String getDescription() {
		return "shows the bounding box of map data";
	}
	
	private static final Color BB_COLOR = Color.YELLOW;
		
	@Override
	protected void renderToImpl(GL gl, Camera camera) {
		
		JOGLTarget target = new JOGLTarget(gl);

		List<VectorXZ> vs = map.getBoundary().polygonXZ().getVertexLoop();
		target.drawLineLoop(BB_COLOR, VectorXZ.listXYZ(vs, 0));
				
	}
	
}
