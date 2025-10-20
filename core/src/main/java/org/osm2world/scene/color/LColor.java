package org.osm2world.scene.color;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * a color with linear RGB components between 0.0 and 1.0.
 * Linear components are useful for doing calculations on colors, such as averaging or scaling them.
 */
public class LColor {

	public static final LColor BLACK = new LColor(0f, 0f, 0f);
	public static final LColor WHITE = new LColor(1f, 1f, 1f);

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

	public static LColor fromRGB(Color color) {
		return new LColor(new float[] {
			(float) srgbToLinear(color.getRed()),
			(float) srgbToLinear(color.getGreen()),
			(float) srgbToLinear(color.getBlue())
		});
	}

	/**
	 * Convert sRGB component (0-255) to linear RGB (0.0-1.0)
	 */
	private static double srgbToLinear(int component) {
		double normalized = component / 255.0;
		if (normalized <= 0.04045) {
			return normalized / 12.92;
		} else {
			return Math.pow((normalized + 0.055) / 1.055, 2.4);
		}
	}

	public Color toRGB() {
		return new Color(linearToSrgb(red), linearToSrgb(green), linearToSrgb(blue));
	}

	/**
	 * Convert linear RGB component (0.0-1.0) to sRGB (0-255)
	 */
	private static int linearToSrgb(double linear) {
		double srgb;
		if (linear <= 0.0031308) {
			srgb = linear * 12.92;
		} else {
			srgb = 1.055 * Math.pow(linear, 1.0 / 2.4) - 0.055;
		}
		return (int) Math.round(Math.max(0, Math.min(255, srgb * 255)));
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
