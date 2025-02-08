package org.osm2world.target.common.texcoord;

import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.target.Target;

/**
 * the function used to calculate texture coordinates for each vertex from
 * a collection. Some implementations only make sense for certain geometries
 * (e.g. vertices forming triangle strips).
 * <p>
 * The origin of OSM2World's texture coordinates is in the lower left corner of a texture image.
 * For output formats with a different convention, {@link Target} need to convert texture coordinates accordingly
 * (e.g. by calculating {@code z = 1.0 - z} for an origin in the top left corner).
 */
@FunctionalInterface
public interface TexCoordFunction {

	/**
	 * calculates a texture coordinate for each vertex
	 */
	public List<VectorXZ> apply(List<VectorXYZ> vs);

}
