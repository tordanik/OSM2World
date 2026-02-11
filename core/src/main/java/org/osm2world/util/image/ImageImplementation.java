package org.osm2world.util.image;

import java.awt.image.BufferedImage;

import org.osm2world.scene.material.TextureData;
import org.osm2world.util.Resolution;

/**
 * implementation of the functionality exposed through {@link ImageUtil}
 */
public interface ImageImplementation {

	/**
	 * Implementation of {@link ImageUtil#loadTextureImage(TextureData, Resolution)}
	 */
	BufferedImage loadTextureImage(TextureData texture, Resolution resolution);

	/**
	 * Implementation of {@link ImageUtil#loadTextureImage(TextureData)}
	 */
	BufferedImage loadTextureImage(TextureData texture);

	/**
	 * Implementation of {@link ImageUtil#getAspectRatio(TextureData)}
	 */
	Float getAspectRatio(TextureData texture);

}
