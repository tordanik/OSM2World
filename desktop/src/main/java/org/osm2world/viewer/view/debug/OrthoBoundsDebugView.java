package org.osm2world.viewer.view.debug;

import java.awt.Color;
import java.util.List;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil.CardinalDirection;
import org.osm2world.core.target.jogl.JOGLTarget;
import org.osm2world.viewer.control.actions.OrthoBoundsAction;

/**
 * illustrates the construction of the orthogonal perspective
 * as set by {@link OrthoBoundsAction}
 */
public class OrthoBoundsDebugView extends DebugView {

	@Override
	public String getDescription() {
		return "illustrates the construction of the orthogonal perspective";
	}

	private static final Color LINE_COLOR = Color.YELLOW;
	private static final Color POINT_COLOR = Color.RED;

	private static final float HALF_POINT_WIDTH = 0.4f;

	@Override
	public boolean canBeUsed() {
		return map != null;
	}

	@Override
	public void fillTarget(JOGLTarget target) {

		Camera orthoCam = OrthoTilesUtil.cameraForBounds(
				map.getDataBoundary(), 30, CardinalDirection.S);

		List<VectorXYZ> boundVertices = map.getDataBoundary().polygonXZ().xyz(0).verticesNoDup();
		target.drawLineLoop(LINE_COLOR, 1, boundVertices);
		target.drawLineStrip(LINE_COLOR, 1, boundVertices.get(0), boundVertices.get(2));
		target.drawLineStrip(LINE_COLOR, 1, boundVertices.get(1), boundVertices.get(3));

		drawBoxAround(target, orthoCam.getPos(), POINT_COLOR, HALF_POINT_WIDTH);
		drawBoxAround(target, orthoCam.getLookAt(), POINT_COLOR, HALF_POINT_WIDTH);

		target.drawLineStrip(LINE_COLOR, 1, orthoCam.getPos(), orthoCam.getLookAt());

	}

}