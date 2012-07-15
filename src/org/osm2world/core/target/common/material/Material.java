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
	
	public static enum Transparency {
		/** arbitrary transparency, including partially transparent pixels */
		TRUE,
		/** only allow pixels to be either fully transparent or fully opaque */
		BINARY,
		/** all pixels are opaque */
		FALSE
	}
	
	protected Lighting lighting;
	protected Color color;
	protected float ambientFactor;
	protected float diffuseFactor;
	protected Transparency transparency;
	
	protected List<TextureData> textureDataList;

	public Material(Lighting lighting, Color color,
			float ambientFactor, float diffuseFactor,
			Transparency transparency, List<TextureData> textureDataList) {
		this.lighting = lighting;
		this.color = color;
		this.ambientFactor = ambientFactor;
		this.diffuseFactor = diffuseFactor;
		this.transparency = transparency;
		this.textureDataList = textureDataList;
	}
	
	public Material(Lighting lighting, Color color,
			Transparency transparency, List<TextureData> textureDataList) {
		this(lighting, color, 0.5f, 0.5f, transparency, textureDataList);
	}
	
	public Material(Lighting lighting, Color color) {
		this(lighting, color, Transparency.FALSE,
				Collections.<TextureData>emptyList());
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
				getTransparency(), getTextureDataList());
	}
	
	public Material darker() {
		return new ImmutableMaterial(lighting, getColor().darker(),
				getAmbientFactor(), getDiffuseFactor(),
				getTransparency(), getTextureDataList());
	}
	
	public static final Color multiplyColor(Color c, float factor) {
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
				getTransparency(), getTextureDataList());
	}

	public Transparency getTransparency() {
		return transparency;
	}
		
	public List<TextureData> getTextureDataList() {
		return textureDataList;
	}
	
	public String toString() {
		return String.format("{%s, #%06x, a%3f, d%3f, %d tex",
				lighting, color.getRGB() & 0x00ffffff, ambientFactor,
				diffuseFactor, textureDataList.size())
				+ transparency
				+ "}";
	}
	
	/*
	 * some possible later additions: specular (obvious ...),
	 * as well as brilliance, phong, metallic, reflection, crand and iridescence for POVRay
	 */
	
}
