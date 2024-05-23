package org.osm2world.core.target.common.material;

import javax.annotation.Nullable;

/**
 * the dimensions of a texture.
 * @param width  width of the texture in meters, greater than 0
 * @param height  height of the texture in meters, greater than 0
 * @param widthPerEntity  for textures containing distinct, repeating objects (e.g. floor tiles),
 *                        this is the width of one such object. Some calculations for texture coords will use this
 *                        to fit an integer number of such objects onto a surface.
 * @param heightPerEntity  vertical counterpart to widthPerEntity
 */
public record TextureDataDimensions(double width, double height,
									@Nullable Double widthPerEntity,
									@Nullable Double heightPerEntity) {

	public TextureDataDimensions {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("Illegal texture dimensions. Width: " + width + ", height: " + height);
		} else if (widthPerEntity != null && widthPerEntity <= 0 || heightPerEntity != null && heightPerEntity <= 0) {
			throw new IllegalArgumentException("Illegal per-entity texture dimensions.");
		}
	}

	public TextureDataDimensions(double width, double height) {
		this(width, height, null, null);
	}

}
