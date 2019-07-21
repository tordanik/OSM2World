package org.osm2world.viewer.view.debug;

import java.awt.Color;

import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.PolygonXZ;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;

/**
 * shows the axis-aligned bounding boxes of the terrain boundaries
 */
public class TerrainBoundaryAABBDebugView extends DebugView {

	@Override
	public String getDescription() {
		return "shows the axis-aligned bounding boxes of the terrain boundaries";
	}

	@Override
	public boolean canBeUsed() {
		return map != null;
	}

	private static final Color BB_COLOR = Color.RED;

	@Override
	protected void fillTarget(JOGLTarget target) {

		for (TerrainBoundaryWorldObject tb :
			map.getWorldObjects(TerrainBoundaryWorldObject.class)) {

			AxisAlignedBoundingBoxXZ box = tb.getAxisAlignedBoundingBoxXZ();
			if (box != null) {
				PolygonXZ polygon = box.polygonXZ();
				target.drawLineLoop(BB_COLOR, 1, polygon.xyz(0).getVertices());
			}

		}

	}

}
