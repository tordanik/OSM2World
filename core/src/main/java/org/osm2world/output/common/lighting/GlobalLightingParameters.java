package org.osm2world.output.common.lighting;

import java.awt.Color;

import org.osm2world.math.VectorXYZ;

/**
 * parameters that describe lighting affecting the entire scene; immutable
 */
public class GlobalLightingParameters {

	public final Color globalAmbientColor;

	/**
	 * source of the scene's directional lighting;
	 * null disables it and leaves only ambient lighting
	 */
	public final VectorXYZ lightFromDirection;

	public final Color lightColorDiffuse;
	public final Color lightColorSpecular;

	private GlobalLightingParameters(
			Color globalAmbientLight, VectorXYZ lightFromDirection,
			Color lightColorDiffuse, Color lightColorSpecular) {

		this.globalAmbientColor = globalAmbientLight;
		this.lightFromDirection = lightFromDirection;
		this.lightColorDiffuse = lightColorDiffuse;
		this.lightColorSpecular = lightColorSpecular;

	}

	public static final GlobalLightingParameters DEFAULT =
			new GlobalLightingParameters(
					new Color(1.0f, 1.0f, 1.0f),
					new VectorXYZ(1.0, 1.5, -1.0),
					Color.WHITE,
					Color.WHITE);

}
