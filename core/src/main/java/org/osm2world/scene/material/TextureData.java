package org.osm2world.scene.material;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.util.Arrays.stream;
import static org.apache.commons.lang3.math.NumberUtils.min;
import static org.osm2world.scene.material.RasterImageFormat.JPEG;
import static org.osm2world.scene.material.RasterImageFormat.PNG;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.osm2world.math.VectorXZ;
import org.osm2world.scene.texcoord.TexCoordFunction;
import org.osm2world.util.Resolution;
import org.osm2world.scene.color.LColor;

/**
 * a texture with metadata necessary for calculating texture coordinates.
 */
public abstract class TextureData {

	public static enum Wrap {

		/** should behave like glTF's "repeat" */
		REPEAT,

		/** should behave like glTF's "clamp to edge" */
		CLAMP;

		public double apply(double d) {
			if (this == REPEAT) {
				double result = d % 1;
				while (result < 0) result += 1.0;
				return result;
			} else {
				return min(max(0.0, d), 1.0);
			}
		}

		public VectorXZ apply(VectorXZ v) {
			return new VectorXZ(apply(v.x), apply(v.z));
		}

	}

	public final TextureDataDimensions dimensions;

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

	protected TextureData(TextureDataDimensions dimensions,
			Wrap wrap, @Nullable  Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {
		this.dimensions = dimensions;
		this.wrap = wrap;
		this.coordFunction = texCoordFunction == null ? null : texCoordFunction.apply(this.dimensions());
	}

	public TextureDataDimensions dimensions() {
		return dimensions;
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
		RasterImageFormat format = getRasterImageFormat();
		try (var stream = new ByteArrayOutputStream()) {
		    writeRasterImageToStream(stream);
		    return "data:" + format.mimeType() + ";base64," + Base64.getEncoder().encodeToString(stream.toByteArray());
		} catch (IOException e) {
		    throw new Error(e);
		}
	}

	/**
	 * returns the format this texture should have when written as a raster image,
	 * e.g. with {@link #getDataUri()} or {@link #writeRasterImageToStream(OutputStream, float)}
	 */
	public RasterImageFormat getRasterImageFormat() {
		return getBufferedImage().getColorModel().hasAlpha() ? PNG : JPEG;
	}

	/**
	 * writes this texture as a raster image to the output stream.
	 * Uses the format returned by {@link #getRasterImageFormat()}
	 *
	 * @param compressionQuality  value between 0 and 1 indicating the desired quality
	 */
	public void writeRasterImageToStream(OutputStream stream, float compressionQuality) throws IOException {

		BufferedImage bufferedImage = getBufferedImage();
		RasterImageFormat format = getRasterImageFormat();

		if (format == JPEG) {

			/* use an implementation which allows setting JPEG compression quality */

			ImageWriter jpegWriter = ImageIO.getImageWritersByMIMEType(JPEG.mimeType()).next();
			ImageWriteParam jpegWriteParam = jpegWriter.getDefaultWriteParam();
			jpegWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			jpegWriteParam.setCompressionQuality(compressionQuality);

			try (var imageOutputStream = new MemoryCacheImageOutputStream(stream)) {
				jpegWriter.setOutput(imageOutputStream);
				var outputImage = new IIOImage(bufferedImage, null, null);
				jpegWriter.write(null, outputImage, jpegWriteParam);
				jpegWriter.dispose();
			}

		} else {
			ImageIO.write(bufferedImage, format.imageIOFormatName(), stream);
		}

	}

	/** variant of {@link #writeRasterImageToStream(OutputStream, float)} with default compression quality */
	public void writeRasterImageToStream(OutputStream stream) throws IOException {
		writeRasterImageToStream(stream, 0.75f);
	}

	/** returns this texture's aspect ratio (same definition as {@link Resolution#getAspectRatio()}) */
	public float getAspectRatio() {
		return Resolution.of(getBufferedImage()).getAspectRatio();
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

	public LColor getColorAt(VectorXZ texCoord, Wrap wrap) {

		texCoord = wrap.apply(texCoord);

		BufferedImage image = getBufferedImage();
		int x = min((int)floor(image.getWidth() * texCoord.x), image.getWidth() - 1);
		int y = min((int)floor(image.getHeight() * texCoord.z), image.getHeight() - 1);
		return LColor.fromAWT(new Color(image.getRGB(x, y)));

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
