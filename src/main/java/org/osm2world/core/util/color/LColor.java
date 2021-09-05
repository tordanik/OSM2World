package org.osm2world.core.util.color;

import java.awt.Color;
import java.awt.color.ColorSpace;

/**
 * a color with linear RGB components between 0.0 and 1.0.
 * Linear components are useful for doing calculations on colors, such as averaging or scaling them.
 */
public class LColor {

	private static final ColorSpace LINEAR_COLOR_SPACE = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);

	public final float red;
	public final float green;
	public final float blue;

	public LColor(float red, float green, float blue) {
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	public Color toAWT() {
		return new Color(LINEAR_COLOR_SPACE, new float[] {red, green, blue}, 1.0f);
	}

	public static LColor fromAWT(Color color) {
		float[] components = color.getColorComponents(LINEAR_COLOR_SPACE, null);
		return new LColor(components[0], components[1], components[2]);
	}

}
