package org.osm2world.viewer.view.debug;

import java.awt.Color;

import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.core.world.modules.building.Building;
import org.osm2world.core.world.modules.building.BuildingPart;
import org.osm2world.core.world.modules.building.roof.HeightfieldRoof;

public class RoofDataDebugView extends DebugView {

	private static final Color INNER_POINT_COLOR = Color.YELLOW;
	private static final Color INNER_SEGMENT_COLOR = Color.GREEN;
	private static final Color POLYGON_COLOR = Color.BLUE;

	@Override
	public boolean canBeUsed() {
		return map != null;
	}

	@Override
	public void fillTarget(JOGLTarget target) {

		for (Building building : map.getWorldObjects(Building.class)) {
			for (BuildingPart part : building.getParts()) {

				if (!(part.getRoof() instanceof HeightfieldRoof)) return;

				HeightfieldRoof roofData = (HeightfieldRoof)part.getRoof();

				for (SimplePolygonXZ polygon : roofData.getPolygon().getPolygons()) {
					for (VectorXZ v : polygon.getVertices()) {
						drawBoxAround(target, v, POLYGON_COLOR, 0.3f);
					}
					for (LineSegmentXZ s : polygon.getSegments()) {
						target.drawLineStrip(POLYGON_COLOR, 1, s.p1.xyz(0), s.p2.xyz(0));
					}
				}

				for (VectorXZ v : roofData.getInnerPoints()) {
					drawBoxAround(target, v, INNER_POINT_COLOR, 0.5f);
				}

				for (LineSegmentXZ s : roofData.getInnerSegments()) {
					target.drawLineStrip(INNER_SEGMENT_COLOR, 1, s.p1.xyz(0), s.p2.xyz(0));
				}

			}

		}

	}

}
