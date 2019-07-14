package org.osm2world.core.target.common.material;

import java.awt.Color;
import java.util.List;

import org.osm2world.core.target.common.TextureData;


/**
 * a material whose attributes can be configured at runtime.
 */
public class ConfMaterial extends Material {

	public ConfMaterial(Interpolation interpolation, Color color,
			float ambientFactor, float diffuseFactor, float specularFactor, int shininess,
			Transparency transparency, Shadow shadow, AmbientOcclusion ao, List<TextureData> textureDataList) {
		super(interpolation, color, ambientFactor, diffuseFactor, specularFactor, shininess,
				transparency, shadow, ao, textureDataList);
	}
	
	public ConfMaterial(Interpolation interpolation, Color color,
			float ambientFactor, float diffuseFactor,
			Transparency transparency, List<TextureData> textureDataList) {
		super(interpolation, color, ambientFactor, diffuseFactor, 0.0f, 1,
				transparency, Shadow.TRUE, AmbientOcclusion.TRUE, textureDataList);
	}
	
	public ConfMaterial(Interpolation interpolation, Color color,
			Transparency transparency, List<TextureData> textureDataList) {
		super(interpolation, color, transparency, textureDataList);
	}
	
	public ConfMaterial(Interpolation interpolation, Color color) {
		super(interpolation, color);
	}
	
	public void setInterpolation(Interpolation interpolation) {
		this.interpolation = interpolation;
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	public void setAmbientFactor(float ambientFactor) {
		this.ambientFactor = ambientFactor;
	}
	
	public void setDiffuseFactor(float diffuseFactor) {
		this.diffuseFactor = diffuseFactor;
	}
	
	public void setSpecularFactor(float specularFactor) {
		this.specularFactor = specularFactor;
	}
	
	public void setShininess(int shininess) {
		this.shininess = shininess;
	}
	
	public void setTransparency(Transparency transparency) {
		this.transparency = transparency;
	}	
	
	public void setShadow(Shadow shadow) {
		this.shadow = shadow;
	}
	
	public void setAmbientOcclusion(AmbientOcclusion ao) {
		this.ambientOcclusion = ao;
	}
	
	public void setTextureDataList(List<TextureData> textureDataList) {
		this.textureDataList = textureDataList;
		this.updateBumpMap();
	}
	
	/*
	 * unlike ImmutableMaterial, this has no equals method.
	 * It should not equal another material just because that one currently (!)
	 * has the same visual parameters.
	 */
	
}
