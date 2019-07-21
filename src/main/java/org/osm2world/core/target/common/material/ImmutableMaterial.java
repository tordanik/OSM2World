package org.osm2world.core.target.common.material;

import java.awt.Color;
import java.util.List;

import org.osm2world.core.target.common.TextureData;

/**
 * a simple material class that offers no capabilities beyond the minimum
 * requirements of {@link Material}
 */
public final class ImmutableMaterial extends Material {

	public ImmutableMaterial(Interpolation interpolation, Color color,
			float ambientFactor, float diffuseFactor, float specularFactor, int shininess,
			Transparency transparency, Shadow shadow, AmbientOcclusion ao, List<TextureData> textureDataList) {
		super(interpolation, color, ambientFactor, diffuseFactor, specularFactor, shininess,
				transparency, shadow, ao, textureDataList);
	}

	public ImmutableMaterial(Interpolation interpolation, Color color,
			Transparency transparency, List<TextureData> textureDataList) {
		super(interpolation, color, transparency, textureDataList);
	}

	public ImmutableMaterial(Interpolation interpolation, Color color) {
		super(interpolation, color);
	}

	// auto-generated
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(ambientFactor);
		result = prime
				* result
				+ ((ambientOcclusion == null) ? 0 : ambientOcclusion.hashCode());
		result = prime * result + ((bumpMap == null) ? 0 : bumpMap.hashCode());
		result = prime * result + bumpMapInd;
		result = prime * result + ((color == null) ? 0 : color.hashCode());
		result = prime * result + Float.floatToIntBits(diffuseFactor);
		result = prime * result
				+ ((interpolation == null) ? 0 : interpolation.hashCode());
		result = prime * result + ((shadow == null) ? 0 : shadow.hashCode());
		result = prime * result + Float.floatToIntBits(shininess);
		result = prime * result + Float.floatToIntBits(specularFactor);
		result = prime * result
				+ ((textureDataList == null) ? 0 : textureDataList.hashCode());
		result = prime * result
				+ ((transparency == null) ? 0 : transparency.hashCode());
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
		if (Float.floatToIntBits(ambientFactor) != Float
				.floatToIntBits(other.ambientFactor))
			return false;
		if (ambientOcclusion != other.ambientOcclusion)
			return false;
		if (bumpMap == null) {
			if (other.bumpMap != null)
				return false;
		} else if (!bumpMap.equals(other.bumpMap))
			return false;
		if (bumpMapInd != other.bumpMapInd)
			return false;
		if (color == null) {
			if (other.color != null)
				return false;
		} else if (!color.equals(other.color))
			return false;
		if (Float.floatToIntBits(diffuseFactor) != Float
				.floatToIntBits(other.diffuseFactor))
			return false;
		if (interpolation != other.interpolation)
			return false;
		if (shadow != other.shadow)
			return false;
		if (Float.floatToIntBits(shininess) != Float
				.floatToIntBits(other.shininess))
			return false;
		if (Float.floatToIntBits(specularFactor) != Float
				.floatToIntBits(other.specularFactor))
			return false;
		if (textureDataList == null) {
			if (other.textureDataList != null)
				return false;
		} else if (!textureDataList.equals(other.textureDataList))
			return false;
		if (transparency != other.transparency)
			return false;
		return true;
	}

}
