package org.osm2world.core.target.common.material;

import static java.lang.Math.*;
import static java.util.Arrays.stream;
import static org.apache.commons.lang3.math.NumberUtils.min;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.texcoord.TexCoordFunction;
import org.osm2world.core.util.Resolution;
import org.osm2world.core.util.color.LColor;

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
	public final @Nullable TexCoordFunction coordFunction;

	/** cached result of {@link #getAverageColor()} */
	private LColor averageColor = null;

	/** cached result of {@link #getBufferedImage()} */
	private BufferedImage bufferedImage = null;

	/** cached result of {@link #getBufferedImage(Resolution)} */
	private final Map<Resolution, BufferedImage> bufferedImageByResolution = new HashMap<>();

	protected TextureData(double width, double height, @Nullable Double widthPerEntity, @Nullable Double heightPerEntity,
			Wrap wrap, @Nullable  Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {

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
		this.coordFunction = texCoordFunction == null ? null : texCoordFunction.apply(this.dimensions());

	}

	public TextureDataDimensions dimensions() {
		return new TextureDataDimensions(width, height, widthPerEntity, heightPerEntity);
	}

	/**
	 * returns the texture as a {@link BufferedImage}.
	 * This may involve converting a vector or procedural texture into a raster image,
	 * or simply reading an existing raster image from a file.
	 *
	 * @param resolution  parameter to request a specific resolution
	 */
	public final BufferedImage getBufferedImage(Resolution resolution) {
		if (!bufferedImageByResolution.containsKey(resolution)) {
			bufferedImageByResolution.put(resolution, createBufferedImage(resolution));
		}
		return bufferedImageByResolution.get(resolution);
	}

	/** see {@link #getBufferedImage(Resolution)} */
	public final BufferedImage getBufferedImage() {
		if (bufferedImage == null) {
			bufferedImage = createBufferedImage();
			bufferedImageByResolution.put(Resolution.of(bufferedImage), bufferedImage);
		}
		return bufferedImage;
	}

	protected BufferedImage createBufferedImage(Resolution resolution) {
		return getScaledImage(getBufferedImage(), resolution);
	}

	protected abstract BufferedImage createBufferedImage();

	/**
	 * returns the texture as a data URI containing a raster image.
	 */
	public String getDataUri() {
		return imageToDataUri(getBufferedImage(), "png");
	}

	/** averages the color values (in linear color space) */
	public LColor getAverageColor() {

		if (averageColor == null) {

			ColorSpace linearCS = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);

			BufferedImage linearRgbImage = new ColorConvertOp(linearCS, null).filter(getBufferedImage(), null);
			int w = linearRgbImage.getWidth();
			int h = linearRgbImage.getHeight();

			double[] redValues = linearRgbImage.getData().getSamples(0, 0, w, h, 0, new double[w * h]);
			double[] greenValues = linearRgbImage.getData().getSamples(0, 0, w, h, 1, new double[w * h]);
			double[] blueValues = linearRgbImage.getData().getSamples(0, 0, w, h, 2, new double[w * h]);

			float redAverage = (float) stream(redValues).average().getAsDouble();
			float greenAverage = (float) stream(greenValues).average().getAsDouble();
			float blueAverage = (float) stream(blueValues).average().getAsDouble();

			redAverage = min(max(redAverage / 255f, 0.0f), 1.0f);
			greenAverage = min(max(greenAverage / 255f, 0.0f), 1.0f);
			blueAverage = min(max(blueAverage / 255f, 0.0f), 1.0f);

			averageColor = new LColor(redAverage, greenAverage, blueAverage);

		}

		return averageColor;

	}

	public LColor getColorAt(VectorXZ texCoord) {

		double texX = texCoord.x % 1;
		double texZ = texCoord.z % 1;

		while (texX < 0) texX += 1.0;
		while (texZ < 0) texZ += 1.0;

		BufferedImage image = getBufferedImage();
		int x = min((int)floor(image.getWidth() * texX), image.getWidth() - 1);
		int y = min((int)floor(image.getHeight() * texZ), image.getHeight() - 1);
		return LColor.fromAWT(new Color(image.getRGB(x, y)));

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
