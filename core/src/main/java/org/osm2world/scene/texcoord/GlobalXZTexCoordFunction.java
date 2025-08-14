package org.osm2world.scene.texcoord;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.scene.material.TextureDataDimensions;

/**
 * uses x and z vertex coords together with the texture's width and height
 * to place a texture. This function works for all geometries,
 * but steep inclines or even vertical walls produce odd-looking results.
 */
public record GlobalXZTexCoordFunction(TextureDataDimensions textureDimensions) implements TexCoordFunction {

	@Override
	public List<VectorXZ> apply(List<VectorXYZ> vs) {

		List<VectorXZ> result = new ArrayList<>(vs.size());

		for (VectorXYZ v : vs) {
			result.add(textureDimensions.applyPadding(new VectorXZ(
							v.x / textureDimensions.width(),
							v.z / textureDimensions.height())));
		}

		return result;

	}

}
