package org.osm2world.scene.material;

import java.util.List;

import org.osm2world.scene.color.Color;


/**
 * a material whose attributes can be configured at runtime.
 */
class MutableMaterial extends Material {

	public MutableMaterial(Interpolation interpolation, Color color, boolean doubleSided,
			Transparency transparency, Shadow shadow, AmbientOcclusion ambientOcclusion,
			List<TextureLayer> textureLayers) {
		super(interpolation, color, doubleSided,
				transparency, shadow, ambientOcclusion, textureLayers);
	}

	public MutableMaterial(Interpolation interpolation, Color color) {
		super(interpolation, color);
	}

	public void setInterpolation(Interpolation interpolation) {
		this.interpolation = interpolation;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void setDoubleSided(boolean doubleSided) {
		this.doubleSided = doubleSided;
	}

	public void setTransparency(Transparency transparency) {
		this.transparency = transparency;
	}

	public void setShadow(Shadow shadow) {
		this.shadow = shadow;
	}

	public void setAmbientOcclusion(AmbientOcclusion ambientOcclusion) {
		this.ambientOcclusion = ambientOcclusion;
	}

	public void setTextureLayers(List<TextureLayer> textureLayers) {
		if (textureLayers.size() > MAX_TEXTURE_LAYERS) {
			throw new IllegalArgumentException("too many texture layers: " + textureLayers.size());
		}
		this.textureLayers = textureLayers;
	}

	@Override
	public String toString() {
		String name = Materials.getUniqueName(this);
		if (name != null) {
			return name;
		} else {
			return super.toString();
		}
	}

	/*
	 * unlike ImmutableMaterial, this has no equals method.
	 * It should not equal another material just because that one currently (!)
	 * has the same visual parameters.
	 */

}
