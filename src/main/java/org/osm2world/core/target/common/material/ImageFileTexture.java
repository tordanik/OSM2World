package org.osm2world.core.target.common.material;

import java.io.File;

import javax.annotation.Nullable;

import com.google.common.base.Objects;

public abstract class ImageFileTexture extends TextureData {

	/**
	 * Path to the texture file.
	 * Represents a permanent, already saved image file in contrast to {@link RuntimeTexture}'s temporary image file.
	 */
	protected final File file;

	protected ImageFileTexture(File file, double width, double height, @Nullable Double widthPerEntity,
			@Nullable Double heightPerEntity, Wrap wrap, TexCoordFunction texCoordFunction) {
		super(width, height, widthPerEntity, heightPerEntity, wrap, texCoordFunction);
		this.file = file;
	}

	public static ImageFileTexture create(File file, double width, double height, @Nullable Double widthPerEntity,
			@Nullable Double heightPerEntity, Wrap wrap, TexCoordFunction texCoordFunction) {
		if (file.getName().endsWith(".svg")) {
			return new SvgImageFileTexture(file, width, height, widthPerEntity, heightPerEntity, wrap, texCoordFunction);
		} else {
			return new RasterImageFileTexture(file, width, height, widthPerEntity, heightPerEntity, wrap, texCoordFunction);
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