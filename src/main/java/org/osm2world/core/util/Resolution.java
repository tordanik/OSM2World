package org.osm2world.core.util;

import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** the dimensions of a raster image */
public class Resolution {

	/** pattern for parseable arguments */
	public static final String PATTERN = "([0-9]{1,9})[×x,]([0-9]{1,9})";

	/** horizontal resolution in pixels */
	public final int width;
	/** vertical resolution in pixels */
	public final int height;

	/** regular constructor */
	public Resolution(int width, int height) {
		this.width = width;
		this.height = height;
		validateValues();
	}

	/**
	 * parsing constructor
	 * @param arg  command line argument to be parsed, needs to match the format of {@link #PATTERN}
	 */
	public Resolution(String arg) {
		Matcher m = Pattern.compile(PATTERN).matcher(arg);
		if (m.matches()) {
			width = Integer.parseInt(m.group(1));
			height = Integer.parseInt(m.group(2));
			validateValues();
		} else {
			throw new IllegalArgumentException("argument doesn't match: " + arg);
		}
	}

	/** @throws IllegalArgumentException  for incorrect field values */
	private void validateValues() {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("Invalid resolution: " + width + "x" + height);
		}
	}

	public static Resolution of(BufferedImage image) {
		return new Resolution(image.getWidth(), image.getHeight());
	}

	public float getAspectRatio() {
		return width / height;
	}

	@Override
	public String toString() {
		return width + "×" + height;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + height;
		result = prime * result + width;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Resolution other = (Resolution) obj;
		if (height != other.height)
			return false;
		if (width != other.width)
			return false;
		return true;
	}

}
