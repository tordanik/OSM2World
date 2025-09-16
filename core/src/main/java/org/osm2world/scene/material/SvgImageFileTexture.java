package org.osm2world.scene.material;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.osm2world.scene.texcoord.TexCoordFunction;
import org.osm2world.util.Resolution;

public class SvgImageFileTexture extends ImageFileTexture {

	public static final Resolution DEFAULT_SVG_RESOLUTION = new Resolution(512, 512);

	public SvgImageFileTexture(File file, TextureDataDimensions dimensions,
			Wrap wrap, Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {
		super(file, dimensions, wrap, texCoordFunction);
	}

	@Override
	public float getAspectRatio() {
		try {
			return (float) getSvgAspectRatio(file);
		} catch (IOException | TranscoderException e) {
			throw new Error("Could not read texture file " + file, e);
		}
	}

	@Override
	protected BufferedImage createBufferedImage() {
		return createBufferedImage(DEFAULT_SVG_RESOLUTION);
	}

	@Override
	protected BufferedImage createBufferedImage(Resolution resolution) {
		try {
			return svgToBufferedImage(this.file);
		} catch (IOException e) {
			throw new Error("Could not read texture file " + file, e);
		}
	}

	/** variant of {@link #svgToBufferedImage(File, Resolution)} that uses a default, power-of-two image size */
	private static final BufferedImage svgToBufferedImage(File svg) throws IOException {
		return svgToBufferedImage(svg, DEFAULT_SVG_RESOLUTION);
	}

	/**
	 * Converts an .svg image file to a raster image and returns it
	 *
	 * @param svgFile  the svg file to be converted
	 */
	private static final BufferedImage svgToBufferedImage(File svgFile, Resolution resolution) throws IOException {

		try {

			/* first conversion (temporary result to determine the SVG's aspect ratio) */

			double inputAspectRatio = getSvgAspectRatio(svgFile);
			double outputAspectRatio = resolution.getAspectRatio();

			/* second conversion (to produce the actual output image) */

			Map<TranscodingHints.Key, Object> transcodingHints;

			if (outputAspectRatio > inputAspectRatio) {
				transcodingHints = singletonMap(PNGTranscoder.KEY_WIDTH, (float) resolution.width);
			} else {
				transcodingHints = singletonMap(PNGTranscoder.KEY_HEIGHT, (float) resolution.height);
			}

			BufferedImage outputImage = svgToBufferedImageImpl(svgFile, transcodingHints);

			/* scale the output image to the desired resolution */

			outputImage = getScaledImage(outputImage, resolution);

			return outputImage;

		} catch (TranscoderException e) {
			throw new IOException(e);
		}

	}

	private static final BufferedImage svgToBufferedImageImpl(File svgFile,
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

	/** returns the aspect ratio of an SVG image file */
	private static double getSvgAspectRatio(File svgFile) throws IOException, TranscoderException {
		BufferedImage tmpImage = svgToBufferedImageImpl(svgFile, emptyMap());
		return Resolution.of(tmpImage).getAspectRatio();
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
		SvgImageFileTexture other = (SvgImageFileTexture) obj;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		return true;
	}
}