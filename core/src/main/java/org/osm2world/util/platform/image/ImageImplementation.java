package org.osm2world.util.platform.image;

import java.awt.image.BufferedImage;

import org.osm2world.scene.material.TextureData;
import org.osm2world.util.Resolution;

/**
 * Implementation of the functionality exposed through {@link ImageUtil}.
 */
interface ImageImplementation {

	/**
	 * Implementation of {@link ImageUtil#loadTextureImage(TextureData, Resolution)}
	 */
	BufferedImage loadTextureImage(TextureData texture, Resolution resolution);

	/**
	 * Implementation of {@link ImageUtil#loadTextureImage(TextureData)}
	 */
	BufferedImage loadTextureImage(TextureData texture);

	/**
	 * Implementation of {@link ImageUtil#getNativeResolution(TextureData)}.
	 * The default implementation loads the image, implementations should override it if they can do better.
	 */
	default Resolution getNativeResolution(TextureData texture) {
		return Resolution.of(loadTextureImage(texture));
	}

	/**
	 * Implementation of {@link ImageUtil#getAspectRatio(TextureData)}
	 */
	Float getAspectRatio(TextureData texture);

}
