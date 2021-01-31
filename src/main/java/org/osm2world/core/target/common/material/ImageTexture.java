package org.osm2world.core.target.common.material;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

public class ImageTexture extends TextureData {

	/**
	 * Path to the texture file.
	 * Represents a permanent, already saved
	 * image file in contrast to the file
	 * field in {@link TextTexture}
	 */
	private final File file;

	private File convertedToPng = null;

	public ImageTexture(File file, double width, double height, Wrap wrap, TexCoordFunction texCoordFunction) {
		super(width, height, wrap, texCoordFunction);
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
		ImageTexture other = (ImageTexture) obj;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		return true;
	}
}