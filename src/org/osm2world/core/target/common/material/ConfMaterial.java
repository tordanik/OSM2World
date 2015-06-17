package org.osm2world.core.target.common.material;

import java.awt.Color;
import java.util.List;

import org.osm2world.core.target.common.TextureData;


/**
 * a material whose attributes can be configured at runtime.
 */
public class ConfMaterial extends Material {

	public ConfMaterial(Interpolation interpolation, Color color,
			float ambientFactor, float diffuseFactor,
			Transparency transparency, List<TextureData> textureDataList) {
		super(interpolation, color, ambientFactor, diffuseFactor,
				transparency, textureDataList);
	}
	
	public ConfMaterial(Interpolation lighting, Color color,
			Transparency transparency, List<TextureData> textureDataList) {
		super(lighting, color, transparency, textureDataList);
	}
	
	public ConfMaterial(Interpolation lighting, Color color) {
		super(lighting, color);
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
	
	public void setTransparency(Transparency transparency) {
		this.transparency = transparency;
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
