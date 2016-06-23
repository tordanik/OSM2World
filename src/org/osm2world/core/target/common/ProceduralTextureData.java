package org.osm2world.core.target.common;

import java.awt.Color;

public class ProceduralTextureData extends TextureData {
	public final float xScale;
	public final float yScale;

	public final Color baseColor;
	// deviation is not the color of the texture, it is the maximum deviation from the base color on each channel
	public final Color deviation;

	public ProceduralTextureData(float xScale, float yScale, Color baseColor, Color deviation, boolean isBumpMap) {
		super(null, 1.0, 1.0, null, null, false, isBumpMap, true);
		this.xScale = xScale;
		this.yScale = yScale;
		this.baseColor = baseColor == null ? new Color(1.0f, 1.0f, 1.0f) : baseColor;
		this.deviation = deviation == null ? new Color(0.04f, 0.04f, 0.03f) : deviation;
	}

	public ProceduralTextureData() {
		this(1.0f, 1.0f, Color.WHITE, null, false);
	}

	public ProceduralTextureData(float xScale, float yScale, Color baseColor, Color deviation) {
		this(xScale, yScale, baseColor, deviation, false);
	}

	@Override
	public String toString() {
		return "ProceduralTextureData [xScale=" + xScale + ", yScale=" + yScale + ", baseColor=" + baseColor + ", deviation=" +deviation + "]";
	}
}

