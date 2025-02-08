package org.osm2world.util.color;

import static com.google.common.base.Preconditions.checkArgument;

import java.awt.*;
import java.awt.color.ColorSpace;

/**
 * a color with linear RGB components between 0.0 and 1.0.
 * Linear components are useful for doing calculations on colors, such as averaging or scaling them.
 */
public class LColor {

	public static final LColor BLACK = new LColor(0f, 0f, 0f);
	public static final LColor WHITE = new LColor(1f, 1f, 1f);

	private static final ColorSpace LINEAR_COLOR_SPACE = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);

	public final float red;
	public final float green;
	public final float blue;

	public LColor(float red, float green, float blue) {

		this.red = red;
		this.green = green;
		this.blue = blue;

		checkArgument(red >= 0 && red <= 1);
		checkArgument(green >= 0 && green <= 1);
		checkArgument(blue >= 0 && blue <= 1);

	}

	/** @param componentsRGB  array with red, green and blue values at indices 0, 1 and 2 */
	public LColor(float[] componentsRGB) {
		this(componentsRGB[0], componentsRGB[1], componentsRGB[2]);
	}

	public float[] componentsRGB() {
		return new float[] {red, green, blue};
	}

	public float[] componentsRGBA() {
		return new float[] {red, green, blue, 1.0f};
	}

	public LColor multiply(LColor color) {
		return new LColor(red * color.red, green * color.green, blue * color.blue);
	}

	public Color toAWT() {
		return new Color(LINEAR_COLOR_SPACE, componentsRGB(), 1.0f);
	}

	public static LColor fromAWT(Color color) {
		float[] componentsRGB = color.getColorComponents(LINEAR_COLOR_SPACE, null);
		return new LColor(componentsRGB);
	}

	@Override
	public String toString() {
		return "(" + red + ", " + green + ", " + blue + ")";
	}

	@Override
	public final boolean equals(Object o) {
		return this == o || o instanceof LColor other
				&& Float.compare(red, other.red) == 0
				&& Float.compare(green, other.green) == 0
				&& Float.compare(blue, other.blue) == 0;
	}

	@Override
	public int hashCode() {
		int result = Float.hashCode(red);
		result = 31 * result + Float.hashCode(green);
		result = 31 * result + Float.hashCode(blue);
		return result;
	}
}
