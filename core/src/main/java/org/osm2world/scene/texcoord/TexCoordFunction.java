package org.osm2world.scene.texcoord;

import java.util.List;
import java.util.function.Function;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.output.Output;
import org.osm2world.scene.material.TextureDataDimensions;

/**
 * the function used to calculate texture coordinates for each vertex from
 * a collection. Some implementations only make sense for certain geometries
 * (e.g. vertices forming triangle strips).
 * <p>
 * The origin of OSM2World's texture coordinates is in the lower left corner of a texture image.
 * For output formats with a different convention, {@link Output} need to convert texture coordinates accordingly
 * (e.g. by calculating {@code z = 1.0 - z} for an origin in the top left corner).
 */
@FunctionalInterface
public interface TexCoordFunction extends Function<TextureDataDimensions, TexCoordSimpleFunction> {

	/**
	 * calculates a texture coordinate for each vertex
	 *
	 * @param dimensions  the dimensions of the texture
	 */
	List<VectorXZ> apply(TextureDataDimensions dimensions, List<VectorXYZ> vs);

	/**
	 * partially applies this function with a dimensions parameter
	 */
	default TexCoordSimpleFunction apply(TextureDataDimensions dimensions) {
		return vs -> this.apply(dimensions, vs);
	}

}
