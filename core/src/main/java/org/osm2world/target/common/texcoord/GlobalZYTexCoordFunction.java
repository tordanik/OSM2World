package org.osm2world.target.common.texcoord;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.target.common.material.TextureDataDimensions;

/**
 * like {@link GlobalXZTexCoordFunction}, but uses y instead of x dimension.
 * Better suited for certain vertical surfaces.
 */
public record GlobalZYTexCoordFunction(TextureDataDimensions textureDimensions) implements TexCoordFunction {

	@Override
	public List<VectorXZ> apply(List<VectorXYZ> vs) {

		List<VectorXZ> result = new ArrayList<>(vs.size());

		for (VectorXYZ v : vs) {
			result.add(TexCoordUtil.applyPadding(new VectorXZ(
						v.z / textureDimensions.width(),
						v.y / textureDimensions.height()),
					textureDimensions));
		}

		return result;

	}

}
