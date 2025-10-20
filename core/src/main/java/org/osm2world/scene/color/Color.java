package org.osm2world.scene.color;

import static java.lang.Math.*;

import javax.annotation.Nonnull;

/**
 * An RGB or RGBA color.
 * Custom lightweight implementation that works on both JVM and TeaVM, unlike awt.Color.
 */
public record Color(int value) {

	public static final Color WHITE = new Color(255, 255, 255);
	public static final Color BLACK = new Color(0, 0, 0);
	public static final Color RED = new Color(255, 0, 0);
	public static final Color GREEN = new Color(0, 255, 0);
	public static final Color BLUE = new Color(0, 0, 255);
	public static final Color YELLOW = new Color(255, 255, 0);
	public static final Color CYAN = new Color(0, 255, 255);
	public static final Color MAGENTA = new Color(255, 0, 255);
	public static final Color GRAY = new Color(128, 128, 128);
	public static final Color LIGHT_GRAY = new Color(192, 192, 192);
	public static final Color DARK_GRAY = new Color(64, 64, 64);
	public static final Color ORANGE = new Color(255, 200, 0);
	public static final Color PINK = new Color(255, 175, 175);

	public Color(int rgba, boolean hasAlpha) {
		this(hasAlpha ? rgba : (0xff000000 | rgba));
	}

	public Color(int r, int g, int b) {
		this(r, g, b, 255);
	}

	public Color(int r, int g, int b, int a) {
		this(((a & 0xFF) << 24) |
				((r & 0xFF) << 16) |
				((g & 0xFF) << 8) |
				((b & 0xFF)));
	}

	public Color(float r, float g, float b) {
		this(r, g, b, 1f);
	}

	public Color(float r, float g, float b, float a) {
		this(
				round(max(0, min(1, r)) * 255),
				round(max(0, min(1, g)) * 255),
				round(max(0, min(1, b)) * 255),
				round(max(0, min(1, a)) * 255)
		);
	}

	public int getRed() {
		return (value >> 16) & 0xFF;
	}

	public int getGreen() {
		return (value >> 8) & 0xFF;
	}

	public int getBlue() {
		return value & 0xFF;
	}

	public int getAlpha() {
		return (value >> 24) & 0xFF;
	}

	public int getRGB() {
		return value;
	}

	public float[] getColorComponents(float[] components) {

		if (components == null) {
			components = new float[3];
		} else if (components.length < 3) {
			throw new IllegalArgumentException("components array must have length >= 3");
		}

		components[0] = (float) this.getRed() / 255.0F;
		components[1] = (float) this.getGreen() / 255.0F;
		components[2] = (float) this.getBlue() / 255.0F;
		return components;

	}

	public static Color decode(String value) {
		return new Color(Integer.decode(value), false);
	}

	public static Color getHSBColor(float h, float s, float b) {
		int r, g, bl;
		if (s == 0f) {
			int v = (int) (b * 255.0f + 0.5f);
			r = g = bl = v;
		} else {
			float hue = (h - (float) floor(h)) * 6.0f;
			float f = hue - (float) floor(hue);
			float p = b * (1.0f - s);
			float q = b * (1.0f - s * f);
			float t = b * (1.0f - s * (1.0f - f));
			switch ((int) hue) {
				case 0:
					r = (int) (b * 255.0f + 0.5f);
					g = (int) (t * 255.0f + 0.5f);
					bl = (int) (p * 255.0f + 0.5f);
					break;
				case 1:
					r = (int) (q * 255.0f + 0.5f);
					g = (int) (b * 255.0f + 0.5f);
					bl = (int) (p * 255.0f + 0.5f);
					break;
				case 2:
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (b * 255.0f + 0.5f);
					bl = (int) (t * 255.0f + 0.5f);
					break;
				case 3:
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (q * 255.0f + 0.5f);
					bl = (int) (b * 255.0f + 0.5f);
					break;
				case 4:
					r = (int) (t * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					bl = (int) (b * 255.0f + 0.5f);
					break;
				default:
					r = (int) (b * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					bl = (int) (q * 255.0f + 0.5f);
			}
		}
		int rgb = 0xff000000 | (r << 16) | (g << 8) | bl;
		return new Color(rgb, true);
	}

	@Override
	public @Nonnull String toString() {
		return String.format("Color[r=%d,g=%d,b=%d,a=%d]",
				getRed(), getGreen(), getBlue(), getAlpha());
	}

}