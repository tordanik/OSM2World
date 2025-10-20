package org.osm2world.viewer.view.debug;

import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.geo.CardinalDirection;
import org.osm2world.output.common.rendering.MutableCamera;
import org.osm2world.output.common.rendering.OrthographicUtil;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.scene.color.Color;
import org.osm2world.viewer.control.actions.OrthoBoundsAction;

/**
 * illustrates the construction of the orthogonal perspective
 * as set by {@link OrthoBoundsAction}
 */
public class OrthoBoundsDebugView extends StaticDebugView {

	public OrthoBoundsDebugView() {
		super("Orthographic bounds debug view", "illustrates the construction of the orthogonal perspective");
	}

	private static final Color LINE_COLOR = Color.YELLOW;
	private static final Color POINT_COLOR = Color.RED;

	private static final float HALF_POINT_WIDTH = 0.4f;

	@Override
	public void fillOutput(JOGLOutput output) {

		MutableCamera orthoCam = OrthographicUtil.cameraForBounds(
				scene.getBoundary(), 30, CardinalDirection.S);

		List<VectorXYZ> boundVertices = scene.getBoundary().polygonXZ().xyz(0).verticesNoDup();
		output.drawLineLoop(LINE_COLOR, 1, boundVertices);
		output.drawLineStrip(LINE_COLOR, 1, boundVertices.get(0), boundVertices.get(2));
		output.drawLineStrip(LINE_COLOR, 1, boundVertices.get(1), boundVertices.get(3));

		drawBoxAround(output, orthoCam.pos(), POINT_COLOR, HALF_POINT_WIDTH);
		drawBoxAround(output, orthoCam.lookAt(), POINT_COLOR, HALF_POINT_WIDTH);

		output.drawLineStrip(LINE_COLOR, 1, orthoCam.pos(), orthoCam.lookAt());

	}

}