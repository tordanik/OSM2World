package org.osm2world.util.platform.image;

import java.awt.*;
import java.awt.image.BufferedImage;

import org.osm2world.scene.material.TextureData;
import org.osm2world.util.Resolution;

/**
 * Utility class for loading images from files and other data.
 * Internally uses separate implementations for use on the browser and on JVM.
 * Most importantly, this allows platform-specific implementations of texture handling.
 */
public class ImageUtil {

	private static ImageImplementation implementation = null;

	/**
	 * Globally sets a different {@link ImageImplementation}.
	 * This exists to support in-browser use.
	 * Most code should never call this method.
	 */
	static void setImplementation(ImageImplementation implementation) {
		ImageUtil.implementation = implementation;
	}

	/**
	 * Returns the texture as a {@link BufferedImage}.
	 * This may involve converting a vector or procedural texture into a raster image,
	 * or simply reading an existing raster image from a file.
	 *
	 * @param resolution  parameter to request a specific resolution
	 */
	public static BufferedImage loadTextureImage(TextureData texture, Resolution resolution) {

		if (implementation == null) {
			throw new UnsupportedOperationException("No platform-specific image implementation provided");
		}

		return implementation.loadTextureImage(texture, resolution);

	}

	/**
	 * See {@link #loadTextureImage(TextureData, Resolution)}
	 */
	public static BufferedImage loadTextureImage(TextureData texture) {

		if (implementation == null) {
			throw new UnsupportedOperationException("No platform-specific image implementation provided");
		}

		return implementation.loadTextureImage(texture);

	}

	/**
	 * Returns this texture's aspect ratio (same definition as {@link Resolution#getAspectRatio()}).
	 * Where possible and applicable, this will return the aspect ratio of the original underlying image.
	 *
	 * @return aspect ratio of this texture, null if the aspect ratio cannot be determined
	 * (e.g. because the texture cannot be loaded in the current environment)
	 */
	public static Float getAspectRatio(TextureData texture) {

		if (implementation == null) {
			return null;
		} else {
			return implementation.getAspectRatio(texture);
		}

	}

	public static BufferedImage getScaledImage(BufferedImage originalImage, Resolution newResolution) {
		Image tmp = originalImage.getScaledInstance(newResolution.width, newResolution.height, Image.SCALE_SMOOTH);
		BufferedImage result = new BufferedImage(newResolution.width, newResolution.height, originalImage.getType());
		Graphics2D g2d = result.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();
		return result;
	}

}
