package org.osm2world.scene.texcoord;

import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.scene.material.TextureDataDimensions;

/**
 * a {@link TexCoordFunction} which does not depend on the {@link TextureDataDimensions}
 */
@FunctionalInterface
public interface TexCoordSimpleFunction extends TexCoordFunction {

	/**
	 * calculates a texture coordinate for each vertex
	 */
	List<VectorXZ> apply(List<VectorXYZ> vs);

	@Override
	default List<VectorXZ> apply(TextureDataDimensions dimensions, List<VectorXYZ> vs) {
		return apply(vs);
	}

	@Override
	default TexCoordSimpleFunction apply(TextureDataDimensions dimensions) {
		return this;
	}

}
