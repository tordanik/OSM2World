package org.osm2world.scene.material;

import java.net.URI;
import java.util.Objects;
import java.util.function.Function;

import org.osm2world.scene.texcoord.TexCoordFunction;

public class UriTexture extends TextureData {

	private final URI imageUri;

	public UriTexture(URI imageUri, TextureDataDimensions dimensions, Wrap wrap,
			Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {
		super(dimensions, wrap, texCoordFunction);
		this.imageUri = imageUri;
	}

	/** returns the URI of the texture image */
	public URI getUri() {
		return imageUri;
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