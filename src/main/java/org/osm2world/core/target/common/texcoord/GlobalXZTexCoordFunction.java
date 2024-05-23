package org.osm2world.core.target.common.texcoord;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.TextureDataDimensions;

/**
 * uses x and z vertex coords together with the texture's width and height
 * to place a texture. This function works for all geometries,
 * but steep inclines or even vertical walls produce odd-looking results.
 */
public class GlobalXZTexCoordFunction implements TexCoordFunction {

	public final TextureDataDimensions textureDimensions;

	public GlobalXZTexCoordFunction(TextureDataDimensions textureDimensions) {
		this.textureDimensions = textureDimensions;
	}

	@Override
	public List<VectorXZ> apply(List<VectorXYZ> vs) {

		List<VectorXZ> result = new ArrayList<>(vs.size());

		for (VectorXYZ v : vs) {
			result.add(new VectorXZ(
					v.x / textureDimensions.width(),
					v.z / textureDimensions.height()));
		}

		return result;

	}

}
