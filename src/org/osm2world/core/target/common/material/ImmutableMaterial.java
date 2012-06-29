package org.osm2world.core.target.common.material;

import java.awt.Color;
import java.util.List;

import org.osm2world.core.target.common.TextureData;

/**
 * a simple material class that offers no capabilities beyond the minimum
 * requirements of {@link Material}
 */
public final class ImmutableMaterial extends Material {
	
	public ImmutableMaterial(Lighting lighting, Color color,
			float ambientFactor, float diffuseFactor,
			boolean useAlpha, List<TextureData> textureDataList) {
		super(lighting, color, ambientFactor, diffuseFactor,
				useAlpha, textureDataList);
	}
	
	public ImmutableMaterial(Lighting lighting, Color color,
			boolean useAlpha, List<TextureData> textureDataList) {
		super(lighting, color, useAlpha, textureDataList);
	}
	
	public ImmutableMaterial(Lighting lighting, Color color) {
		super(lighting, color);
	}
		
	// auto-generated
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(ambientFactor);
		result = prime * result + ((color == null) ? 0 : color.hashCode());
		result = prime * result + Float.floatToIntBits(diffuseFactor);
		result = prime * result + ((lighting == null) ? 0 : lighting.hashCode());
		result = prime * result + ((textureDataList == null) ? 0 : textureDataList.hashCode());
		result = prime * result + (useAlpha ? 1231 : 1237);
		return result;
	}

	// auto-generated
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Material other = (Material) obj;
		if (Float.floatToIntBits(ambientFactor) != Float.floatToIntBits(other.ambientFactor))
			return false;
		if (color == null) {
			if (other.color != null)
				return false;
		} else if (!color.equals(other.color))
			return false;
		if (Float.floatToIntBits(diffuseFactor) != Float.floatToIntBits(other.diffuseFactor))
			return false;
		if (lighting != other.lighting)
			return false;
		if (textureDataList == null) {
			if (other.textureDataList != null)
				return false;
		} else if (!textureDataList.equals(other.textureDataList))
			return false;
		if (useAlpha != other.useAlpha)
			return false;
		return true;
	}
	
}
