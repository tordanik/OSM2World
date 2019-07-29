package org.osm2world.core.target.common;

import java.awt.Color;

public class ProceduralTextureData extends TextureData {
	public final float xScale;
	public final float yScale;

	public final Color baseColor;
	// deviation is not the color of the texture, it is the maximum deviation from the base color on each channel
	public final Color deviation;

	public final float normalDeviation;

	public ProceduralTextureData(float xScale, float yScale
							   , Color baseColor, Color deviation
							   , float normalDeviation) {
		super(null, 1.0, 1.0, null, null, false, false, true, false);
		this.xScale = xScale;
		this.yScale = yScale;
		this.baseColor = baseColor == null ? new Color(1.0f, 1.0f, 1.0f) : baseColor;
		this.deviation = deviation == null ? new Color(0.04f, 0.04f, 0.03f) : deviation;
		this.normalDeviation = normalDeviation;
	}

	public ProceduralTextureData() {
		this(1.0f, 1.0f, Color.WHITE, null, 0.0f);
	}

	public ProceduralTextureData(float xScale, float yScale, Color baseColor, Color deviation) {
		this(xScale, yScale, baseColor, deviation, 0.0f);
	}

	@Override
	public String toString() {
		return "ProceduralTextureData [xScale=" + xScale + ", yScale=" + yScale + ", baseColor=" + baseColor + ", deviation=" +deviation + "]";
	}
}

