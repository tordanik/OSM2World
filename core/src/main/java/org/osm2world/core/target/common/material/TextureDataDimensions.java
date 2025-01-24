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
 * @param padding  the fraction of the texture which represents padding, greater or equal 0 and less than 0.5.
 *                 Padding is only used for a minority of textures (0 otherwise), only for textures
 *                 which aren't repeated, and typically for textures which have a transparent border.
 *                 For example, if this value is 0.1, only the portion of the image between texture coordinates
 *                 0.1 and 0.9 (along both the height and width axis) represents actual content.
 *                 The goal of padding is to avoid colors bleeding into the border on coarse mip-map levels.
 */
public record TextureDataDimensions(double width, double height,
									@Nullable Double widthPerEntity,
									@Nullable Double heightPerEntity,
									double padding) {

	public TextureDataDimensions {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("Illegal texture dimensions. Width: " + width + ", height: " + height);
		} else if (widthPerEntity != null && widthPerEntity <= 0 || heightPerEntity != null && heightPerEntity <= 0) {
			throw new IllegalArgumentException("Illegal per-entity texture dimensions.");
		} else if (padding < 0 || padding >= 0.5) {
			throw new IllegalArgumentException("Illegal texture dimensions. Padding: " + padding);
		}
	}

	public TextureDataDimensions(double width, double height) {
		this(width, height, null, null, 0);
	}

}
