package org.osm2world.target.common.material;

import java.util.function.Function;

import org.osm2world.target.common.texcoord.TexCoordFunction;

/**
 * a texture that is only generated (or turned into an image from some input data) during application runtime
 */
public abstract class RuntimeTexture extends TextureData {

	protected RuntimeTexture(TextureDataDimensions dimensions, Wrap wrap,
			Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {
		super(dimensions, wrap, texCoordFunction);
	}

}
