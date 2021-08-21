package org.osm2world.core.target.common.material;

/**
 * a texture that is only generated (or turned into an image from some input data) during application runtime
 */
public abstract class RuntimeTexture extends TextureData {

	protected RuntimeTexture(double width, double height, Double widthPerEntity, Double heightPerEntity, Wrap wrap,
			TexCoordFunction texCoordFunction) {
		super(width, height, widthPerEntity, heightPerEntity, wrap, texCoordFunction);
	}

}
