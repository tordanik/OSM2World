package org.osm2world.scene.material;

import java.util.List;

import org.osm2world.scene.color.Color;

/**
 * a simple material class that offers no capabilities beyond the minimum
 * requirements of {@link Material}
 */
public final class ImmutableMaterial extends Material {

	public ImmutableMaterial(Interpolation interpolation, Color color, boolean doubleSided,
			Transparency transparency, Shadow shadow, AmbientOcclusion ambientOcclusion,
			List<TextureLayer> textureLayers) {
		super(interpolation, color, doubleSided,
				transparency, shadow, ambientOcclusion, textureLayers);
	}

	public ImmutableMaterial(Interpolation interpolation, Color color,
			Transparency transparency, List<TextureLayer> textureLayers) {
		super(interpolation, color, transparency, textureLayers);
	}

	public ImmutableMaterial(Interpolation interpolation, Color color) {
		super(interpolation, color);
	}

	// auto-generated
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((ambientOcclusion == null) ? 0 : ambientOcclusion.hashCode());
		result = prime * result + ((color == null) ? 0 : color.hashCode());
		result = prime * result
				+ ((interpolation == null) ? 0 : interpolation.hashCode());
		result = prime * result + ((shadow == null) ? 0 : shadow.hashCode());
		result = prime * result
				+ ((textureLayers == null) ? 0 : textureLayers.hashCode());
		result = prime * result
				+ ((transparency == null) ? 0 : transparency.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} if (!(obj instanceof ImmutableMaterial)) {
			return false;
		} else {
			return this.equals((ImmutableMaterial) obj, false, false);
		}
	}

}
