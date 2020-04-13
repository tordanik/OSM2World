package org.osm2world.core.target.common.material;

import java.util.List;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.TextureData;

/**
 * the function used to calculate texture coordinates for each vertex from
 * a collection. Some implementations only make sense for certain geometries
 * (e.g. vertices forming triangle strips).
 */
@FunctionalInterface
public interface TexCoordFunction {

	/**
	 * calculates a texture coordinate for each vertex
	 */
	public List<VectorXZ> apply(List<VectorXYZ> vs, TextureData textureData);

}
