package org.osm2world.core.target.common.material;

import java.awt.Color;

/**
 * describes the material/surface properties of an object for lighting
 */
public abstract class Material {
	
	public static enum Lighting {FLAT, SMOOTH};
	
	protected Lighting lighting;
	protected Color color;
	protected float ambientFactor;
	protected float diffuseFactor;
	
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
		
	public Lighting getLighting() {
		return lighting;
	}
	
	public Color getColor() {
		return color;
	}
	
	public float getAmbientFactor() {
		return ambientFactor;
	}
	
	public float getDiffuseFactor() {
		return diffuseFactor;
	}
		
	public Color ambientColor() {
		return multiplyColor(getColor(), getAmbientFactor());
	}
	
	public Color diffuseColor() {
		return multiplyColor(getColor(), getDiffuseFactor());
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
		return new ImmutableMaterial(Lighting.SMOOTH, getColor(),
				getAmbientFactor(), getDiffuseFactor());
	}
	
	public String toString() {
		return "{" + lighting + ", " + color + ", a" + ambientFactor +
			", d" + diffuseFactor + "}";
	}
		
	/*
	 * some possible later additions: specular (obvious ...),
	 * as well as brilliance, phong, metallic, reflection, crand and iridescence for POVRay
	 */
	
}
