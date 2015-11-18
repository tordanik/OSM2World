package org.osm2world.core.target.common.material;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osm2world.core.target.common.TextureData;

/**
 * describes the material/surface properties of an object for lighting
 */
public abstract class Material {
	
	public static enum Interpolation {FLAT, SMOOTH};
	
	public static enum Transparency {
		/** arbitrary transparency, including partially transparent pixels */
		TRUE,
		/** only allow pixels to be either fully transparent or fully opaque */
		BINARY,
		/** all pixels are opaque */
		FALSE
	}
	
	public static enum Shadow {
		/** casts shadows */
		TRUE,
		/** casts no shadows */
		FALSE
	}
	
	public static enum AmbientOcclusion {
		/** casts AO */
		TRUE,
		/** casts no AO */
		FALSE
	}
	
	/**
	 * Interpolation of normals
	 */
	protected Interpolation interpolation;
	protected Color color;
	protected float ambientFactor;
	protected float diffuseFactor;
	protected float specularFactor;
	protected int shininess;
	protected Transparency transparency;
	protected Shadow shadow;
	protected AmbientOcclusion ambientOcclusion;
	
	protected List<TextureData> textureDataList;
	protected TextureData bumpMap;
	protected int bumpMapInd;

	public Material(Interpolation interpolation, Color color,
			float ambientFactor, float diffuseFactor, float specularFactor, int shininess, 
			Transparency transparency, Shadow shadow, AmbientOcclusion ao, List<TextureData> textureDataList) {
		this.interpolation = interpolation;
		this.color = color;
		this.ambientFactor = ambientFactor;
		this.diffuseFactor = diffuseFactor;
		this.specularFactor = specularFactor;
		this.shininess = shininess;
		this.transparency = transparency;
		this.shadow = shadow;
		this.ambientOcclusion = ao;
		this.textureDataList = textureDataList;
		updateBumpMap();
	}
	
	protected void updateBumpMap() {
		this.bumpMap = null;
		this.bumpMapInd = -1;
		if (textureDataList == null) {
			return;
		}
		int i = 0;
		for (TextureData t : textureDataList) {
			if (t.isBumpMap) {
				this.bumpMap = t;
				this.bumpMapInd = i;
			}
			i++;
		}
	}
	
	public Material(Interpolation interpolation, Color color,
			Transparency transparency, List<TextureData> textureDataList) {
		this(interpolation, color, 0.5f, 0.5f, 0.0f, 1, transparency,
				Shadow.TRUE, AmbientOcclusion.TRUE, textureDataList);
	}
	
	public Material(Interpolation interpolation, Color color) {
		this(interpolation, color, Transparency.FALSE,
				Collections.<TextureData>emptyList());
	}
		
	public Interpolation getInterpolation() {
		return interpolation;
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
	
	public float getSpecularFactor() {
		return specularFactor;
	}
	
	public int getShininess() {
		return shininess;
	}
		
	public Color ambientColor() {
		return multiplyColor(getColor(), getAmbientFactor());
	}
	
	public Color diffuseColor() {
		return multiplyColor(getColor(), getDiffuseFactor());
	}
	
	public Material brighter() {
		return new ImmutableMaterial(interpolation, getColor().brighter(),
				getAmbientFactor(), getDiffuseFactor(), getSpecularFactor(), getShininess(),
				getTransparency(), getShadow(), getAmbientOcclusion(), getTextureDataList());
	}
	
	public Material darker() {
		return new ImmutableMaterial(interpolation, getColor().darker(),
				getAmbientFactor(), getDiffuseFactor(), getSpecularFactor(), getShininess(),
				getTransparency(), getShadow(), getAmbientOcclusion(), getTextureDataList());
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
		return new ImmutableMaterial(Interpolation.SMOOTH, getColor(),
				getAmbientFactor(), getDiffuseFactor(), getSpecularFactor(), getShininess(),
				getTransparency(), getShadow(), getAmbientOcclusion(), getTextureDataList());
	}
	
	/**
	 * returns a material that is the same as this one,
	 * except with additional texture data layers stacked on top
	 */
	public Material withAddedLayers(List<TextureData> textureLayers) {
		
		if (textureLayers.isEmpty()) return this;
		
		List<TextureData> textureDataList =
				new ArrayList<TextureData>(getTextureDataList());
	    
		textureDataList.addAll(textureLayers);
	    
	    return new ImmutableMaterial(getInterpolation(), getColor(),
	    		getAmbientFactor(), getDiffuseFactor(), getSpecularFactor(), getShininess(),
	    		getTransparency(), getShadow(), getAmbientOcclusion(), textureDataList);
	    
	}
	
	public Transparency getTransparency() {
		return transparency;
	}
	
	public Shadow getShadow() {
		return shadow;
	}
	
	public AmbientOcclusion getAmbientOcclusion() {
		return ambientOcclusion;
	}
		
	public List<TextureData> getTextureDataList() {
		return textureDataList;
	}
	
	public int getNumTextureLayers() {
		if (textureDataList == null) {
			return 0;
		} else {
			return textureDataList.size();
		}
	}

	public boolean hasBumpMap() {
		return this.bumpMap != null;
	}
	
	public TextureData getBumpMap() {
		return this.bumpMap;
	}
	
	public int getBumpMapInd() {
		return this.bumpMapInd;
	}
	
	public String toString() {
		return String.format("{%s, #%06x, a%3f, d%3f, s%3f, sh%3f, %d tex",
				interpolation, color.getRGB() & 0x00ffffff, ambientFactor,
				diffuseFactor, specularFactor, shininess, textureDataList.size())
				+ transparency + shadow + ambientOcclusion
				+ "}";
	}
	
	/*
	 * some possible later additions: specular (obvious ...),
	 * as well as brilliance, phong, metallic, reflection, crand and iridescence for POVRay
	 */
	
}
