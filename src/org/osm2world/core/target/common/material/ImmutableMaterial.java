package org.osm2world.core.target.common.material;

import java.awt.Color;

/**
 * a simple material class that offers no capabilities beyond the minimum
 * requirements of {@link Material}
 */
public final class ImmutableMaterial extends Material {
		
	public ImmutableMaterial(Lighting lighting, Color color,
			float ambientFactor, float diffuseFactor) {
		super(lighting, color, ambientFactor, diffuseFactor);
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
		result = prime * result
		+ ((lighting == null) ? 0 : lighting.hashCode());
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
		ImmutableMaterial other = (ImmutableMaterial) obj;
		if (Float.floatToIntBits(ambientFactor) != Float
				.floatToIntBits(other.ambientFactor))
			return false;
		if (color == null) {
			if (other.color != null)
				return false;
		} else if (!color.equals(other.color))
			return false;
		if (Float.floatToIntBits(diffuseFactor) != Float
				.floatToIntBits(other.diffuseFactor))
			return false;
		if (lighting == null) {
			if (other.lighting != null)
				return false;
		} else if (!lighting.equals(other.lighting))
			return false;
		return true;
	}
	
}
