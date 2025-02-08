package org.osm2world.viewer.view.debug;

import static org.osm2world.math.VectorXZ.listXYZ;
import static org.osm2world.util.FaultTolerantIterationUtil.forEach;

import java.awt.*;

import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.math.shapes.SimplePolygonShapeXZ;
import org.osm2world.target.jogl.JOGLTarget;
import org.osm2world.world.data.AreaWorldObject;
import org.osm2world.world.data.WaySegmentWorldObject;

/**
 * draws ground footprints defined by world objects
 */
public class GroundFootprintDebugView extends DebugView {

	private static final Color NODE_BOUNDARY_COLOR = Color.YELLOW;
	private static final Color WAY_BOUNDARY_COLOR = Color.GREEN;
	private static final Color AREA_BOUNDARY_COLOR = Color.BLUE;

	@Override
	public String getDescription() {
		return "draws terrain boundaries defined by world objects";
	}

	@Override
	public boolean canBeUsed() {
		return map != null;
	}

	@Override
	public void fillTarget(JOGLTarget target) {

		/* draw terrain boundaries */

		forEach(map.getWorldObjects(), o -> {

			if (o.getGroundState() == GroundState.ON) {

				Color color = NODE_BOUNDARY_COLOR;

				if (o instanceof WaySegmentWorldObject) {
					color = WAY_BOUNDARY_COLOR;
				} else if (o instanceof AreaWorldObject) {
					color = AREA_BOUNDARY_COLOR;
				}

				for (PolygonShapeXZ p : o.getRawGroundFootprint()) {
					for (SimplePolygonShapeXZ ring : p.getRings()) {
						target.drawLineLoop(color, 1, listXYZ(ring.vertices(), 0));
					}
				}

			}

		});

	}

}
