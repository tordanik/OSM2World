package org.osm2world.util.platform.image;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.osm2world.scene.material.TextureData;
import org.osm2world.util.Resolution;

/**
 * An {@link ImageImplementation} which caches previously loaded texture images.
 */
abstract class CachingImageImplementation implements ImageImplementation {

	/** Cached results of {@link #loadTextureImage(TextureData)} ()} */
	private final Map<TextureData, BufferedImage> cachedImages = new HashMap<>();

	/** Cached results of {@link #loadTextureImage(TextureData, Resolution)} */
	private final Map<Pair<TextureData, Resolution>, BufferedImage> cachedImagesByResolution = new HashMap<>();

	@Override
	public BufferedImage loadTextureImage(TextureData texture, Resolution resolution) {
		var key = Pair.of(texture, resolution);
		if (!cachedImagesByResolution.containsKey(key)) {
			cachedImagesByResolution.put(key, createBufferedImage(texture, resolution));
		}
		return cachedImagesByResolution.get(key);
	}

	@Override
	public BufferedImage loadTextureImage(TextureData texture) {
		if (!cachedImages.containsKey(texture)) {
			BufferedImage image = createBufferedImage(texture);
			cachedImages.put(texture, image);
			cachedImagesByResolution.put(Pair.of(texture, Resolution.of(image)), image);
		}
		return cachedImages.get(texture);
	}

	protected abstract BufferedImage createBufferedImage(TextureData texture, Resolution resolution);

	protected abstract BufferedImage createBufferedImage(TextureData texture);

}
