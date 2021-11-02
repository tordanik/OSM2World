package org.osm2world.core.target.common.material;

import java.util.function.Function;

import org.osm2world.core.target.common.texcoord.TexCoordFunction;

/**
 * a texture that is only generated (or turned into an image from some input data) during application runtime
 */
public abstract class RuntimeTexture extends TextureData {

	protected RuntimeTexture(double width, double height, Double widthPerEntity, Double heightPerEntity, Wrap wrap,
			Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {
		super(width, height, widthPerEntity, heightPerEntity, wrap, texCoordFunction);
	}

}
