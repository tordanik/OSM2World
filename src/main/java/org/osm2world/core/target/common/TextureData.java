package org.osm2world.core.target.common;

import java.io.File;

import org.osm2world.core.target.common.material.TexCoordFunction;

/**
 * a texture with all information necessary for applying it to an object
 * that has texture coordinates
 */
public abstract class TextureData {

	public static enum Wrap { REPEAT, CLAMP, CLAMP_TO_BORDER }

	/** width of a single tile of the texture */
	public final double width;

	/** height of a single tile of the texture */
	public final double height;

	/** wrap style of the texture */
	public final Wrap wrap;

	/** calculation rule for texture coordinates */
	public final TexCoordFunction coordFunction;

	/**
	 * whether the texture is modulated with the material color.
	 * Otherwise, a plain white base color is used, resulting in the texture's
	 * colors appearing unaltered (except for lighting)
	 */
	public final boolean colorable;

	public final boolean isBumpMap;

	public TextureData(double width, double height, Wrap wrap,
			TexCoordFunction texCoordFunction, boolean colorable, boolean isBumpMap) {

		this.width = width;
		this.height = height;
		this.wrap = wrap;
		this.coordFunction = texCoordFunction;
		this.colorable = colorable;
		this.isBumpMap = isBumpMap;

	}

	/**
	 * returns the texture as a raster image file (png or jpeg).
	 * If the texture is originally defined as a procedural texture or vector graphics, a temporary file is provided.
	 */
	public abstract File getRasterImage();

	//auto-generated
	@Override
	public String toString() {
		return "TextureData [width=" + width + ", height=" + height + ", wrap=" + wrap
				+ ", texCoordFunction=" + coordFunction + ", colorable=" + colorable + ", isBumpMap=" + isBumpMap + "]";
	}

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object obj);

}
