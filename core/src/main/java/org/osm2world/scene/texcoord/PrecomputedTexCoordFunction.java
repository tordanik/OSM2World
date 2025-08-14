package org.osm2world.scene.texcoord;

import static java.util.Collections.unmodifiableList;

import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.scene.material.TextureDataDimensions;

/**
 * a texture coordinate function that stores a known texture coordinate for each vertex.
 * Unlike {@link MapBasedTexCoordFunction}, this allows vertices in the same location to have different texture coords.
 */
public class PrecomputedTexCoordFunction implements TexCoordFunction {

	public final List<VectorXZ> texCoords;

	public PrecomputedTexCoordFunction(List<VectorXZ> texCoords) {
		this.texCoords = unmodifiableList(texCoords);
	}

	/**
	 * variant constructor where texCoords are raw coordinates for a 1m x 1m texture
	 * and need to be scaled (e.g. halved for 2m x 2m textures)
	 *
	 * @param totalX  size of the range of X values, used for texture snapping, optional
	 * @param totalZ  size of the range of Z values, used for texture snapping, optional
	 */
	public PrecomputedTexCoordFunction(List<VectorXZ> rawTexCoords, TextureDataDimensions textureDimensionsForScaling,
			@Nullable Double totalX, @Nullable Double totalZ) {

		this.texCoords = textureDimensionsForScaling.scaleTexCoords(rawTexCoords, totalX, totalZ);

	}

	@Override
	public List<VectorXZ> apply(List<VectorXYZ> vs) {
		if (vs.size() != texCoords.size()) {
			throw new IllegalArgumentException("incorrect number of vertices");
		}
		return texCoords;
	}

}
