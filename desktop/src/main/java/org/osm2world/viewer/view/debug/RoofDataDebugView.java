package org.osm2world.viewer.view.debug;

import java.awt.*;

import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.LineSegmentXZ;
import org.osm2world.math.shapes.SimplePolygonShapeXZ;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.world.modules.building.Building;
import org.osm2world.world.modules.building.BuildingPart;
import org.osm2world.world.modules.building.roof.HeightfieldRoof;

public class RoofDataDebugView extends DebugView {

	private static final Color INNER_POINT_COLOR = Color.YELLOW;
	private static final Color INNER_SEGMENT_COLOR = Color.GREEN;
	private static final Color POLYGON_COLOR = Color.WHITE;
	private static final Color EXTRA_OUTLINE_COLOR = Color.RED;

	@Override
	public boolean canBeUsed() {
		return scene != null;
	}

	@Override
	public void fillTarget(JOGLOutput target) {

		for (Building building : scene.getWorldObjects(Building.class)) {
			for (BuildingPart part : building.getParts()) {

				if (!(part.getRoof() instanceof HeightfieldRoof roofData)) return;

				for (SimplePolygonShapeXZ polygon : roofData.getPolygon().getRings()) {
					for (VectorXZ v : polygon.verticesNoDup()) {
						boolean isOld = part.getPolygon().getRings().stream().anyMatch(r -> r.vertices().contains(v));
						drawBoxAround(target, v, isOld ? POLYGON_COLOR : EXTRA_OUTLINE_COLOR, 0.3f);
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
