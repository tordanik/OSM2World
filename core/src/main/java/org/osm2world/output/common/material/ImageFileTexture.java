package org.osm2world.output.common.material;

import java.io.File;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.osm2world.output.common.texcoord.TexCoordFunction;

import com.google.common.base.Objects;

public abstract class ImageFileTexture extends TextureData {

	/**
	 * Path to the texture file.
	 * Represents a permanent, already saved image file in contrast to {@link RuntimeTexture}'s temporary image file.
	 */
	protected final File file;

	protected ImageFileTexture(File file, TextureDataDimensions dimensions, Wrap wrap,
			Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {
		super(dimensions, wrap, texCoordFunction);
		this.file = file;
	}

	public static ImageFileTexture create(File file, TextureDataDimensions dimensions, Wrap wrap,
			@Nullable Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {
		if (file.getName().endsWith(".svg")) {
			return new SvgImageFileTexture(file, dimensions, wrap, texCoordFunction);
		} else {
			return new RasterImageFileTexture(file, dimensions, wrap, texCoordFunction);
		}
	}

	public File getFile() {
		return file;
	}

	@Override
	public String toString() {
		return file.getName();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ImageFileTexture
				&& Objects.equal(this.file, ((ImageFileTexture)obj).file);
	}

	@Override
	public int hashCode() {
		return file.hashCode();
	}

}