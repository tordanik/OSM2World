package org.osm2world.core.target.common.material;

import java.awt.Color;
import java.util.List;

import org.osm2world.core.target.common.TextureData;


/**
 * a material whose attributes can be configured at runtime.
 */
public class ConfMaterial extends Material {

	public ConfMaterial(Lighting lighting, Color color,
			float ambientFactor, float diffuseFactor,
			List<TextureData> textureDataList) {
		super(lighting, color, ambientFactor, diffuseFactor, textureDataList);
	}
	
	public ConfMaterial(Lighting lighting, Color color,
			List<TextureData> textureDataList) {
		super(lighting, color, textureDataList);
	}
	
	public ConfMaterial(Lighting lighting, Color color) {
		super(lighting, color);
	}
	
	public void setLighting(Lighting lighting) {
		this.lighting = lighting;
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
	
	public void setTextureDataList(List<TextureData> textureDataList) {
		this.textureDataList = textureDataList;
	}
	
	/*
	 * unlike ImmutableMaterial, this has no equals method.
	 * It should not equal another material just because that one currently (!)
	 * has the same visual parameters.
	 */
	
}
