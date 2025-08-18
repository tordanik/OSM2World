package org.osm2world.viewer.view.debug;

import static java.awt.Color.*;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;

import java.awt.*;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.output.jogl.JOGLOutput;

/**
 * shows the internal world coordinate grid
 */
public class InternalCoordsDebugView extends DebugView {

	private static final double LINE_DIST = 100;

	public InternalCoordsDebugView() {
		super("Metric coordinate grid", "shows the internal world coordinate grid");
	}

	@Override
	public void fillTarget(JOGLOutput target) {

		AxisAlignedRectangleXZ bound = scene.getBoundary();

		for (int x = (int)floor(bound.minX / LINE_DIST); x < (int)ceil(bound.maxX / LINE_DIST); x++) {
			for (int z = (int)floor(bound.minZ / LINE_DIST); z < (int)ceil(bound.maxZ / LINE_DIST); z++) {

				Color colorX = (z == 0 && x >= 0) ? RED : WHITE;
				int widthX = (z == 0) ? 3 : 1;
				target.drawLineStrip(colorX, widthX,
						new VectorXYZ(x * LINE_DIST, 0, z * LINE_DIST),
						new VectorXYZ((x+1) * LINE_DIST, 0, z * LINE_DIST));

				Color colorZ = (x == 0 && z >= 0) ? BLUE : WHITE;
				int widthZ = (x == 0) ? 3 : 1;
				target.drawLineStrip(colorZ, widthZ,
						new VectorXYZ(x * LINE_DIST, 0, z * LINE_DIST),
						new VectorXYZ(x * LINE_DIST, 0, (z+1) * LINE_DIST));

			}
		}

		target.drawLineStrip(GREEN, 3,
				VectorXYZ.NULL_VECTOR, new VectorXYZ(0, LINE_DIST, 0));

	}

}
