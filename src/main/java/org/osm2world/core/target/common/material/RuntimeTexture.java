package org.osm2world.core.target.common.material;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * a texture that is only generated (or turned into an image from some input data) during application runtime
 */
public abstract class RuntimeTexture extends TextureData {

	/** temporary image file, saved until application termination */
	private File tempImageFile = null;

	public RuntimeTexture(double width, double height, Double widthPerEntity, Double heightPerEntity, Wrap wrap,
			TexCoordFunction texCoordFunction) {
		super(width, height, widthPerEntity, heightPerEntity, wrap, texCoordFunction);
	}

	@Override
	public String getDataUri() {
		return imageToDataUri(getBufferedImage(), "png");
	}

	@Override
	public File getRasterImage() {

		if (tempImageFile == null) {
			BufferedImage image = getBufferedImage();
			String prefix = "osm2world";
			this.tempImageFile = createTemporaryPngFile(prefix, image);
		}

		return this.tempImageFile;

	}

	private static File createTemporaryPngFile(String prefix, BufferedImage image) {

		File outputFile = null;

		try {
			outputFile = File.createTempFile(prefix, ".png");
			outputFile.deleteOnExit();
			ImageIO.write(image, "png", outputFile);
		} catch(IOException e) {
			System.err.println("Exception in createPng: " + prefix);
			e.printStackTrace();
		}

		return outputFile;
	}

}
