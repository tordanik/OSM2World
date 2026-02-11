package org.osm2world.scene.material;

import static org.osm2world.scene.material.RasterImageFormat.JPEG;
import static org.osm2world.scene.material.RasterImageFormat.PNG;

import java.io.File;
import java.util.function.Function;

import org.osm2world.scene.texcoord.TexCoordFunction;

public class RasterImageFileTexture extends ImageFileTexture {

	public RasterImageFileTexture(File file, TextureDataDimensions dimensions, Wrap wrap,
								  Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {
		super(file, dimensions, wrap, texCoordFunction);
	}

	@Override
	public RasterImageFormat getRasterImageFormat() {
		return (getFile().getName().endsWith(".png")) ? PNG : JPEG;
	}

}