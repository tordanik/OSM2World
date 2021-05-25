package org.osm2world.core.target.common.material;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

import com.jogamp.opengl.util.awt.ImageUtil;

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
				convertedToPng = SVG2PNG(this.file);
			}

			return convertedToPng;
		}

		return this.file;

	}

	/**
	 * Converts an .svg image file into a (temporary) .png
	 *
	 * @param svg
	 * The svg file to be converted
	 * @return a File object representation of the generated png
	 */
	private File SVG2PNG(File svg) {

		String prefix = svg.getName().substring(0, svg.getName().indexOf('.')) + "osm2World";

		PNGTranscoder t = new PNGTranscoder();

		//create the transcoder input
		String svgURI = svg.toURI().toString();
		TranscoderInput input = new TranscoderInput(svgURI);

		try {

			File outputFile = null;

			//create a temporary file in the default temporary-file directory
			outputFile = File.createTempFile(prefix, ".png");
			outputFile.deleteOnExit();

			try (OutputStream ostream = new FileOutputStream(outputFile)) {

				TranscoderOutput output = new TranscoderOutput(ostream);

				//save the image.
				t.transcode(input, output);

				ostream.flush();
			}

			return outputFile;

		} catch (IOException | TranscoderException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public BufferedImage getBufferedImage() {
		try {
			String format = getRasterImageFileFormat();
			BufferedImage image = ImageIO.read(getRasterImage());
			if ("png".equals(format)) {
				ImageUtil.flipImageVertically(image); //flip to ensure consistent tex coords with png images
			}
			return image;
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