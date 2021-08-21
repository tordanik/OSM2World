package org.osm2world.core.target.common.material;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

public class RasterImageFileTexture extends ImageFileTexture {

	public RasterImageFileTexture(File file, double width, double height, @Nullable Double widthPerEntity,
			@Nullable Double heightPerEntity, Wrap wrap, TexCoordFunction texCoordFunction) {
		super(file, width, height, widthPerEntity, heightPerEntity, wrap, texCoordFunction);
	}

	@Override
	public BufferedImage getBufferedImage() {
		try {
			return ImageIO.read(this.file);
		} catch (IOException e) {
			throw new Error("Could not read texture file " + file, e);
		}
	}

	@Override
	public String getDataUri() {
		return imageToDataUri(getBufferedImage(), getRasterImageFileFormat());
	}

	private String getRasterImageFileFormat() {
		return (getFile().getName().endsWith(".png")) ? "png" : "jpeg";
	}

}