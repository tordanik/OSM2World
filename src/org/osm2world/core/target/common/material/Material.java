package org.osm2world.core.target.common.material;

import java.awt.Color;
import java.util.Collections;
import java.util.List;

import org.osm2world.core.target.common.TextureData;

/**
 * describes the material/surface properties of an object for lighting
 */
public abstract class Material {
	
	public static enum Lighting {FLAT, SMOOTH};
	
	protected Lighting lighting;
	protected Color color;
	protected float ambientFactor;
	protected float diffuseFactor;
	
	protected List<TextureData> textureDataList;

	public Material(Lighting lighting, Color color,
			float ambientFactor, float diffuseFactor,
			List<TextureData> textureDataList) {
		this.lighting = lighting;
		this.color = color;
		this.ambientFactor = ambientFactor;
		this.diffuseFactor = diffuseFactor;
		this.textureDataList = textureDataList;
	}
	
	public Material(Lighting lighting, Color color,
			List<TextureData> textureDataList) {
		this(lighting, color, 0.5f, 0.5f, textureDataList);
	}
	
	public Material(Lighting lighting, Color color) {
		this(lighting, color, Collections.<TextureData>emptyList());
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
	
	public Material brighter() {
		return new ImmutableMaterial(lighting, getColor().brighter(),
				getAmbientFactor(), getDiffuseFactor(),
				getTextureDataList());
	}
	
	public Material darker() {
		return new ImmutableMaterial(lighting, getColor().darker(),
				getAmbientFactor(), getDiffuseFactor(),
				getTextureDataList());
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
				getAmbientFactor(), getDiffuseFactor(),
				getTextureDataList());
	}
		
	public List<TextureData> getTextureDataList() {
		return textureDataList;
	}
	
	public String toString() {
		return String.format("{%s, #%06x, a%3f, d%3f}", lighting,
				color.getRGB() & 0x00ffffff, ambientFactor, diffuseFactor);
	}
		
	/*
	 * some possible later additions: specular (obvious ...),
	 * as well as brilliance, phong, metallic, reflection, crand and iridescence for POVRay
	 */
	
}
