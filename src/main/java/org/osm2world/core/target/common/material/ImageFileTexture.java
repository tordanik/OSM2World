package org.osm2world.core.target.common.material;

import static java.util.Collections.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.PNGTranscoder;

public class ImageFileTexture extends TextureData {

	/**
	 * Path to the texture file.
	 * Represents a permanent, already saved image file in contrast to {@link RuntimeTexture}'s temmporary image file.
	 */
	private final File file;

	private File convertedToPng = null;

	public ImageFileTexture(File file, double width, double height, @Nullable Double widthPerEntity, @Nullable Double heightPerEntity,
			Wrap wrap, TexCoordFunction texCoordFunction) {
		super(width, height, widthPerEntity, heightPerEntity, wrap, texCoordFunction);
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	@Override
	public File getRasterImage() {

		if (this.file.getName().endsWith(".svg")) {

			if (this.convertedToPng == null) {
				convertedToPng = svg2png(this.file);
			}

			return convertedToPng;
		}

		return this.file;

	}

	/** variant of {@link #svg2png(File, int, int)} that uses a default, power-of-two image size */
	private static final File svg2png(File svg) {
		return svg2png(svg, 512, 512);
	}

	/**
	 * Converts an .svg image file into a (temporary) .png
	 *
	 * @param svgFile  the svg file to be converted
	 * @param width  horizontal resolution (in pixels) of the output image
	 * @param height  vertical resolution (in pixels) of the output image
	 */
	private static final File svg2png(File svgFile, int width, int height) {

		if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid resolution: " + width + "x" + height);

		double outputAspectRatio = width / height;

		try {

			/* first conversion (temporary result to determine the SVG's aspect ratio) */

			BufferedImage tmpImage = svgToBufferedImage(svgFile, emptyMap());
			double inputAspectRatio = tmpImage.getWidth() / (float)tmpImage.getHeight();

			/* second conversion (to produce the actual output image) */

			Map<TranscodingHints.Key, Object> transcodingHints;

			if (outputAspectRatio > inputAspectRatio) {
				transcodingHints = singletonMap(PNGTranscoder.KEY_WIDTH, (float) width);
			} else {
				transcodingHints = singletonMap(PNGTranscoder.KEY_HEIGHT, (float) height);
			}

			BufferedImage outputImage = svgToBufferedImage(svgFile, transcodingHints);

			/* scale the output image to the desired resolution */

			outputImage = getScaledImage(outputImage, width, height);

			/* write the output image to a temporary file in the default temporary-file directory */

			File outputFile = File.createTempFile("o2w-" + svgFile.getName().replaceAll("\\.svg$", "-"), ".png");
			outputFile.deleteOnExit();
			ImageIO.write(outputImage, "png", outputFile);

			return outputFile;

		} catch (IOException | TranscoderException e) {
			throw new RuntimeException(e);
		}

	}

	/** returns a raster image representation of an SVG file */
	private static final BufferedImage svgToBufferedImage(File svgFile,
			Map<TranscodingHints.Key, Object> transcodingHints) throws IOException, TranscoderException {

		PNGTranscoder t = new PNGTranscoder();
		t.setTranscodingHints(transcodingHints);

		TranscoderInput input = new TranscoderInput(svgFile.toURI().toString());

		try (ByteArrayOutputStream ostream = new ByteArrayOutputStream()) {

			TranscoderOutput output = new TranscoderOutput(ostream);
			t.transcode(input, output);
			ostream.flush();
			byte[] tempImageBytes = ostream.toByteArray();

			try (ByteArrayInputStream istream = new ByteArrayInputStream(tempImageBytes)) {
				return ImageIO.read(istream);
			}

		}

	}

	@Override
	public BufferedImage getBufferedImage() {
		try {
			return ImageIO.read(getRasterImage());
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	@Override
	public String getDataUri() {
		return imageToDataUri(getBufferedImage(), getRasterImageFileFormat());
	}

	private String getRasterImageFileFormat() {
		return getRasterImage().getName().endsWith(".png") ? "png" : "jpeg";
	}

	@Override
	public String toString() {
		return file.getName();
	}

	//auto-generated
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		return result;
	}

	//auto-generated
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImageFileTexture other = (ImageFileTexture) obj;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		return true;
	}
}