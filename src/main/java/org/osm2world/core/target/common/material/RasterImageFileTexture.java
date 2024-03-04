package org.osm2world.core.target.common.material;

import static org.osm2world.core.target.common.material.RasterImageFormat.JPEG;
import static org.osm2world.core.target.common.material.RasterImageFormat.PNG;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.osm2world.core.target.common.texcoord.TexCoordFunction;

public class RasterImageFileTexture extends ImageFileTexture {

	public RasterImageFileTexture(File file, double width, double height, @Nullable Double widthPerEntity,
								  @Nullable Double heightPerEntity, Wrap wrap,
								  Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {
		super(file, width, height, widthPerEntity, heightPerEntity, wrap, texCoordFunction);
	}

	@Override
	protected BufferedImage createBufferedImage() {
		try {
			return ImageIO.read(this.file);
		} catch (IOException e) {
			throw new Error("Could not read texture file " + file, e);
		}
	}

	@Override
	public RasterImageFormat getRasterImageFormat() {
		return (getFile().getName().endsWith(".png")) ? PNG : JPEG;
	}

}