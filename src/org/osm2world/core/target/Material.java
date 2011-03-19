package org.osm2world.core.target;

import java.awt.Color;

/**
 * describes the material/surface properties of an object for lighting
 */
public class Material {
	
	public static enum Lighting {FLAT, SMOOTH};
	
	public final Lighting lighting;
	public final Color color;
	public final float ambientFactor;
	public final float diffuseFactor;
	
	public Material(Lighting lighting, Color color,
			float ambientFactor, float diffuseFactor) {
		this.lighting = lighting;
		this.color = color;
		this.ambientFactor = ambientFactor;
		this.diffuseFactor = diffuseFactor;
	}

	public Material(Lighting lighting, Color color) {
		this(lighting, color, 0.5f, 0.5f);
	}
	
	public Color ambientColor() {
		return multiplyColor(color, ambientFactor);
	}
	
	public Color diffuseColor() {
		return multiplyColor(color, diffuseFactor);
	}
	
	private static final Color multiplyColor(Color c, float factor) {
		float[] colorComponents = new float[3]; 
		c.getColorComponents(colorComponents);
		
		return new Color(
				colorComponents[0] * factor,
				colorComponents[1] * factor,
				colorComponents[2] * factor);
	}

	public Material makeSmooth() {
		return new Material(Lighting.SMOOTH, color, ambientFactor, diffuseFactor);
	}
	
	@Override
	public String toString() {
		return "{" + lighting + ", " + color + ", a" + ambientFactor + ", d" + diffuseFactor + "}";
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
		Material other = (Material) obj;
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
		
	/*
	 * some possible later additions: specular (obvious ...),
	 * as well as brilliance, phong, metallic, reflection, crand and iridescence for POVRay
	 */	
	
}
