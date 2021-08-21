package org.osm2world.core.target.common.material;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.osm2world.core.util.Resolution;

import jakarta.xml.bind.DatatypeConverter;

/**
 * a texture with metadata necessary for calculating texture coordinates.
 */
public abstract class TextureData {

	public static enum Wrap { REPEAT, CLAMP, CLAMP_TO_BORDER }

	/** width of a single tile of the texture in meters, greater than 0 */
	public final double width;

	/** height of a single tile of the texture in meters, greater than 0 */
	public final double height;

	/**
	 * for textures that contain distinct, repeating objects (e.g. tiles), this describes the width of one such object.
	 * Some calculations for texture coords will use this to fit an integer number of such objects onto a surface.
	 */
	public final @Nullable Double widthPerEntity;

	/** see {@link #widthPerEntity} */
	public final @Nullable Double heightPerEntity;

	/** wrap style of the texture */
	public final Wrap wrap;

	/** calculation rule for texture coordinates */
	public final TexCoordFunction coordFunction;

	protected TextureData(double width, double height, @Nullable Double widthPerEntity, @Nullable Double heightPerEntity,
			Wrap wrap, TexCoordFunction texCoordFunction) {

		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("Illegal texture dimensions. Width: " + width + ", height: " + height);
		} else if (widthPerEntity != null && widthPerEntity <= 0 || heightPerEntity != null && heightPerEntity <= 0) {
			throw new IllegalArgumentException("Illegal per-entity texture dimensions.");
		}

		this.width = width;
		this.height = height;
		this.widthPerEntity = widthPerEntity;
		this.heightPerEntity = heightPerEntity;
		this.wrap = wrap;
		this.coordFunction = texCoordFunction;

	}

	/**
	 * returns the texture as a {@link BufferedImage}.
	 * This may involve converting a vector or procedural texture into a raster image,
	 * or simply reading an existing raster image from a file.
	 *
	 * @param resolution  parameter to request a specific resolution
	 */
	public BufferedImage getBufferedImage(Resolution resolution) {
		return getScaledImage(getBufferedImage(), resolution);
	}

	/** see {@link #getBufferedImage(Resolution)} */
	public abstract BufferedImage getBufferedImage();

	/**
	 * returns the texture as a data URI containing a raster image.
	 */
	public String getDataUri() {
		return imageToDataUri(getBufferedImage(), "png");
	}

	protected static final String imageToDataUri(BufferedImage image, String format) {
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		    ImageIO.write(image, format, byteArrayOutputStream);
		    return "data:image/" + format + ";base64,"
		    		+ DatatypeConverter.printBase64Binary(byteArrayOutputStream.toByteArray());
		} catch (IOException e) {
		    throw new Error(e);
		}
	}

	protected static final BufferedImage getScaledImage(BufferedImage originalImage, Resolution newResolution) {
		Image tmp = originalImage.getScaledInstance(newResolution.width, newResolution.height, Image.SCALE_SMOOTH);
		BufferedImage result = new BufferedImage(newResolution.width, newResolution.height, originalImage.getType());
		Graphics2D g2d = result.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();
		return result;
	}

}
