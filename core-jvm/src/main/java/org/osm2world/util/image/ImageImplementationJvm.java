package org.osm2world.util.image;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.osm2world.util.image.ImageUtil.getScaledImage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.osm2world.conversion.ConversionLog;
import org.osm2world.scene.material.*;
import org.osm2world.util.Resolution;

/**
 * An {@link ImageImplementation} for use on the JVM.
 */
public class ImageImplementationJvm extends CachingImageImplementation {

	private static final Resolution DEFAULT_SVG_RESOLUTION = new Resolution(512, 512);

	/** Sets up {@link ImageUtil} to use this implementation. */
	public static void register() {
		ImageUtil.setImplementation(new ImageImplementationJvm());
	}

	@Override
	protected BufferedImage createBufferedImage(TextureData texture, Resolution resolution) {
		try {
			if (texture instanceof RasterImageFileTexture
					|| texture instanceof UriTexture) {
				return getScaledImage(createBufferedImage(texture), resolution);
			} else if (texture instanceof SvgImageFileTexture svgTexture) {
				return svgToBufferedImage(svgTexture.getFile(), resolution);
			} else if (texture instanceof RuntimeTexture runtimeTexture) {
				return runtimeTexture.createBufferedImage(resolution);
			} else {
				throw new Error("Unsupported texture type: " + texture.getClass().getSimpleName());
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not load image for texture " + texture, e);
		}
	}

	@Override
	protected BufferedImage createBufferedImage(TextureData texture) {
		try {
			if (texture instanceof RasterImageFileTexture rasterTexture) {
				return ImageIO.read(rasterTexture.getFile());
			} else if (texture instanceof UriTexture uriTexture) {
				return ImageIO.read(uriTexture.getUri().toURL());
			} else if (texture instanceof SvgImageFileTexture svgTexture) {
				return svgToBufferedImage(svgTexture.getFile(), DEFAULT_SVG_RESOLUTION);
			} else if (texture instanceof RuntimeTexture runtimeTexture) {
				return runtimeTexture.createBufferedImage();
			} else {
				throw new Error("Unsupported texture type: " + texture.getClass().getSimpleName());
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not load image for texture " + texture, e);
		}
	}

	@Override
	public Float getAspectRatio(TextureData texture) {
		if (texture instanceof SvgImageFileTexture svgTexture) {
			try {
				return (float) getSvgAspectRatio(svgTexture.getFile());
			} catch (IOException | TranscoderException e) {
				ConversionLog.error("Could not read texture file " + svgTexture.getFile(), e);
				return null;
			}
		} else if (texture instanceof RuntimeTexture runtimeTexture) {
			return runtimeTexture.getAspectRatio();
		} else {
			return Resolution.of(loadTextureImage(texture)).getAspectRatio();
		}
	}

	/**
	 * Converts an .svg image file to a raster image and returns it
	 *
	 * @param svgFile  the svg file to be converted
	 */
	private static BufferedImage svgToBufferedImage(File svgFile, Resolution resolution) throws IOException {

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

	private static BufferedImage svgToBufferedImageImpl(File svgFile,
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


}
