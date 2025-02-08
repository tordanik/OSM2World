package org.osm2world.target.common.material;

import java.awt.image.RenderedImage;
import java.io.File;

public enum RasterImageFormat {

	PNG, JPEG;

	/** returns a String that can be used for {@link javax.imageio.ImageIO#write(RenderedImage, String, File)} */
	public String imageIOFormatName() {
		return toString().toLowerCase();
	}

	public String mimeType() {
		return "image/" + toString().toLowerCase();
	}

	public String fileExtension() {
		return switch (this) {
			case PNG -> "png";
			case JPEG -> "jpg";
		};
	}

}
