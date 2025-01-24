package org.osm2world.core.target.jogl;

import java.awt.Color;
import java.util.List;

import org.osm2world.core.math.VectorXYZ;

/**
 * represents points and lines to be drawn by a {@link JOGLTarget}.
 */
class NonAreaPrimitive {

	static enum Type {
		POINTS, LINES, LINE_STRIP, LINE_LOOP
	}

	public final Type type;
	public final Color color;
	public final int width;
	public final List<VectorXYZ> vs;

	public NonAreaPrimitive(Type type, Color color, int width,
			List<VectorXYZ> vs) {

		this.type = type;
		this.color = color;
		this.width = width;
		this.vs = vs;

	}

}
