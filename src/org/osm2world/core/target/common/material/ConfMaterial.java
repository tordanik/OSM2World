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
	
	public ConfMaterial setInterpolation(Interpolation interpolation) {
		this.interpolation = interpolation;
		return this;
	}
	
	public ConfMaterial setColor(Color color) {
		this.color = color;
		return this;
	}
	
	public ConfMaterial setAmbientFactor(float ambientFactor) {
		this.ambientFactor = ambientFactor;
		return this;
	}
	
	public ConfMaterial setDiffuseFactor(float diffuseFactor) {
		this.diffuseFactor = diffuseFactor;
		return this;
	}
	
	public ConfMaterial setSpecularFactor(float specularFactor) {
		this.specularFactor = specularFactor;
		return this;
	}
	
	public ConfMaterial setShininess(int shininess) {
		this.shininess = shininess;
		return this;
	}
	
	public ConfMaterial setTransparency(Transparency transparency) {
		this.transparency = transparency;
		return this;
	}	
	
	public ConfMaterial setShadow(Shadow shadow) {
		this.shadow = shadow;
		return this;
	}
	
	public ConfMaterial setAmbientOcclusion(AmbientOcclusion ao) {
		this.ambientOcclusion = ao;
		return this;
	}
	
	public ConfMaterial setTextureDataList(List<TextureData> textureDataList) {
		this.textureDataList = textureDataList;
		this.updateBumpMap();
		return this;
	}
	
	/*
	 * unlike ImmutableMaterial, this has no equals method.
	 * It should not equal another material just because that one currently (!)
	 * has the same visual parameters.
	 */
	
}
