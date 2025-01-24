package org.osm2world.core.target.common.material;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.osm2world.core.target.common.texcoord.TexCoordFunction;

public class UriTexture extends RuntimeTexture {

	private final URI imageUri;

	public UriTexture(URI imageUri, TextureDataDimensions dimensions, Wrap wrap,
			Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {
		super(dimensions, wrap, texCoordFunction);
		this.imageUri = imageUri;
	}

	@Override protected BufferedImage createBufferedImage() {
		try {
			return ImageIO.read(imageUri.toURL());
		} catch (IOException e) {
			throw new RuntimeException("Could not read texture from URI " + imageUri, e);
		}
	}

	@Override
	public String toString() {
		return imageUri.toString();
	}

	@Override
	public final boolean equals(Object o) {
		return o instanceof UriTexture that
				&& Objects.equals(imageUri, that.imageUri);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(imageUri);
	}

}