package org.osm2world.core.target.common.texcoord;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.TextureDataDimensions;

/**
 * like {@link GlobalXZTexCoordFunction}, but uses y instead of x dimension.
 * Better suited for certain vertical surfaces.
 */
public class GlobalZYTexCoordFunction implements TexCoordFunction {

	public final TextureDataDimensions textureDimensions;

	public GlobalZYTexCoordFunction(TextureDataDimensions textureDimensions) {
		this.textureDimensions = textureDimensions;
	}

	@Override
	public List<VectorXZ> apply(List<VectorXYZ> vs) {

		List<VectorXZ> result = new ArrayList<>(vs.size());

		for (VectorXYZ v : vs) {
			result.add(new VectorXZ(
					v.z / textureDimensions.width(),
					v.y / textureDimensions.height()));
		}

		return result;

	}

}
