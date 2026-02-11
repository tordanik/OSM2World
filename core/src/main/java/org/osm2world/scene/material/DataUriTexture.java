package org.osm2world.scene.material;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.osm2world.scene.texcoord.TexCoordFunction;

public class DataUriTexture extends RuntimeTexture {

	private final String dataUri;

	public DataUriTexture(String dataUri, TextureDataDimensions dimensions, Wrap wrap,
						  @Nullable Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {
		super(dimensions, wrap, texCoordFunction);
		this.dataUri = dataUri;
	}

	@Override
	public BufferedImage createBufferedImage() {

		var pattern = Pattern.compile("data:image/(?:png|jpg|jpeg);base64,(.+)");
		var matcher = pattern.matcher(dataUri);

		if (matcher.matches()) {

			try {

				String encodedData = matcher.group(1);
				byte[] data = Base64.getDecoder().decode(encodedData);
				return ImageIO.read(new ByteArrayInputStream(data));

			} catch (IOException e) {
				throw new Error("Could not read create image from data URI in " + this, e);
			}

		} else {
			throw new Error("Not a valid data URI - " + this);
		}

	}

	@Override
	public String toString() {
		int maxLength = 40;
		boolean tooLong = dataUri.length() > maxLength;
		String substring = tooLong ? dataUri.substring(0, maxLength) + "..." : dataUri;
		return "DataUriTexture(" + substring + ")";
	}

	@Override
	public final boolean equals(Object o) {
		if (!(o instanceof DataUriTexture that)) return false;
		return dataUri.equals(that.dataUri);
	}

	@Override
	public int hashCode() {
		return dataUri.hashCode();
	}

}
