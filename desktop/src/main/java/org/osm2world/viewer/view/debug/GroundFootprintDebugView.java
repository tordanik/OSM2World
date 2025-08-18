package org.osm2world.viewer.view.debug;

import static org.osm2world.math.VectorXZ.listXYZ;
import static org.osm2world.util.FaultTolerantIterationUtil.forEach;

import java.awt.*;

import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.math.shapes.SimplePolygonShapeXZ;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.world.data.AreaWorldObject;
import org.osm2world.world.data.WaySegmentWorldObject;

/**
 * draws ground footprints defined by world objects
 */
public class GroundFootprintDebugView extends StaticDebugView {

	private static final Color NODE_BOUNDARY_COLOR = Color.YELLOW;
	private static final Color WAY_BOUNDARY_COLOR = Color.GREEN;
	private static final Color AREA_BOUNDARY_COLOR = Color.BLUE;

	public GroundFootprintDebugView() {
		super("Ground footprints", "draws terrain boundaries defined by world objects");
	}

	@Override
	public void fillOutput(JOGLOutput output) {

		/* draw terrain boundaries */

		forEach(scene.getWorldObjects(), o -> {

			if (o.getGroundState() == GroundState.ON) {

				Color color = NODE_BOUNDARY_COLOR;

				if (o instanceof WaySegmentWorldObject) {
					color = WAY_BOUNDARY_COLOR;
				} else if (o instanceof AreaWorldObject) {
					color = AREA_BOUNDARY_COLOR;
				}

				for (PolygonShapeXZ p : o.getRawGroundFootprint()) {
					for (SimplePolygonShapeXZ ring : p.getRings()) {
						output.drawLineLoop(color, 1, listXYZ(ring.vertices(), 0));
					}
				}

			}

		});

	}

}
