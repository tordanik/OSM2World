package org.osm2world.core.target.common.material;

import java.io.File;

/**
 * a texture with metadata necessary for calculating tile coordinates.
 *
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

	public TextureData(double width, double height, Wrap wrap,
			TexCoordFunction texCoordFunction) {
		this.width = width;
		this.height = height;
		this.wrap = wrap;
		this.coordFunction = texCoordFunction;
	}

	/**
	 * returns the texture as a raster image file (png or jpeg).
	 * If the texture is originally defined as a procedural texture or vector graphics, a temporary file is provided.
	 */
	public abstract File getRasterImage();

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object obj);

}
