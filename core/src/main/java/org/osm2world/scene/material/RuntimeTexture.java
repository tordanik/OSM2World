package org.osm2world.scene.material;

import java.awt.image.BufferedImage;
import java.util.function.Function;

import org.osm2world.scene.texcoord.TexCoordFunction;
import org.osm2world.util.Resolution;
import org.osm2world.util.image.ImageUtil;

/**
 * a texture that is only generated (or turned into an image from some input data) during application runtime
 */
public abstract class RuntimeTexture extends TextureData {

	protected RuntimeTexture(TextureDataDimensions dimensions, Wrap wrap,
			Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {
		super(dimensions, wrap, texCoordFunction);
	}

	public Float getAspectRatio() {
		return Resolution.of(ImageUtil.loadTextureImage(this)).getAspectRatio();
	}

	abstract public BufferedImage createBufferedImage();

	public BufferedImage createBufferedImage(Resolution resolution) {
		return ImageUtil.getScaledImage(createBufferedImage(), resolution);
	}


}
