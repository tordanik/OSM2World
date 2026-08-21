package org.osm2world.util.platform.image;

import static java.lang.Math.max;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.osm2world.scene.material.TextureData;
import org.osm2world.util.Resolution;

/**
 * An {@link ImageImplementation} which caches previously loaded texture images.
 */
abstract class CachingImageImplementation implements ImageImplementation {

	/** identifies a cached image at a particular resolution. */
	private record CacheKey(TextureData texture, Resolution resolution) {}

	/** approximate maximum total size (in bytes) of the images in {@link #cache} */
	private final long maxCachedBytes;

	/** cached images in least-recently-used order. All access must be synchronized on this object. */
	private final LinkedHashMap<CacheKey, BufferedImage> cache = new LinkedHashMap<>(16, 0.75f, true);

	/**
	 * the resolution which each texture's image has if it is created without requesting a particular resolution.
	 * This is only known after the image has been created once, and is forgotten again when it is evicted from
	 * {@link #cache}. All access must be synchronized on {@link #cache}.
	 */
	private final Map<TextureData, Resolution> nativeResolutions = new HashMap<>();

	/** approximate total size (in bytes) of the images in {@link #cache}, only access while synchronized on it */
	private long cachedBytes = 0;

	/**
	 * @param maxCachedBytes  see {@link #maxCachedBytes}
	 */
	protected CachingImageImplementation(long maxCachedBytes) {
		this.maxCachedBytes = maxCachedBytes;
	}

	@Override
	public BufferedImage loadTextureImage(TextureData texture, Resolution resolution) {

		var key = new CacheKey(texture, resolution);

		synchronized (cache) {
			BufferedImage cachedImage = cache.get(key);
			if (cachedImage != null) {
				return cachedImage;
			}
		}

		/* create the image without holding the lock. Creating an image can be slow, and blocking other threads for
		 * its entire duration would defeat the purpose of running conversions in parallel. As a result, two threads
		 * may occasionally create the same image at the same time; only one of the results ends up in the cache. */

		BufferedImage image = createBufferedImage(texture, resolution);

		synchronized (cache) {
			put(key, image);
			evictUntilWithinSizeLimit();
		}

		return image;

	}

	@Override
	public BufferedImage loadTextureImage(TextureData texture) {

		synchronized (cache) {
			Resolution nativeResolution = nativeResolutions.get(texture);
			if (nativeResolution != null) {
				BufferedImage cachedImage = cache.get(new CacheKey(texture, nativeResolution));
				if (cachedImage != null) {
					return cachedImage;
				}
			}
		}

		BufferedImage image = createBufferedImage(texture); // not while holding the lock, see above

		synchronized (cache) {
			Resolution nativeResolution = Resolution.of(image);
			nativeResolutions.put(texture, nativeResolution);
			put(new CacheKey(texture, nativeResolution), image);
			evictUntilWithinSizeLimit();
		}

		return image;

	}

	/** adds an entry to {@link #cache}, keeping {@link #cachedBytes} up to date. Call while synchronized on cache. */
	private void put(CacheKey key, BufferedImage image) {
		BufferedImage previousImage = cache.put(key, image);
		if (previousImage != null) {
			cachedBytes -= sizeInBytes(previousImage);
		}
		cachedBytes += sizeInBytes(image);
	}

	/** drops the least recently used images until the size limit is met. Call while synchronized on cache. */
	private void evictUntilWithinSizeLimit() {

		var iterator = cache.entrySet().iterator();

		while (cachedBytes > maxCachedBytes && iterator.hasNext()) {

			Map.Entry<CacheKey, BufferedImage> leastRecentlyUsed = iterator.next();
			iterator.remove();

			nativeResolutions.remove(leastRecentlyUsed.getKey().texture());

			cachedBytes -= sizeInBytes(leastRecentlyUsed.getValue());

		}

	}

	/** estimates the amount of memory used by an image */
	private static long sizeInBytes(BufferedImage image) {
		int bytesPerPixel = max(1, image.getColorModel().getPixelSize() / 8);
		return (long) image.getWidth() * image.getHeight() * bytesPerPixel;
	}

	protected abstract BufferedImage createBufferedImage(TextureData texture, Resolution resolution);

	protected abstract BufferedImage createBufferedImage(TextureData texture);

}
