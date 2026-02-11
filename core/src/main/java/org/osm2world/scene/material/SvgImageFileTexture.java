package org.osm2world.scene.material;

import java.io.File;
import java.util.function.Function;

import org.osm2world.scene.texcoord.TexCoordFunction;

public class SvgImageFileTexture extends ImageFileTexture {

	public SvgImageFileTexture(File file, TextureDataDimensions dimensions,
			Wrap wrap, Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {
		super(file, dimensions, wrap, texCoordFunction);
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