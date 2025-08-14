package org.osm2world.scene.texcoord;

import static java.lang.Math.round;
import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
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

		this.texCoords = scaleTexCoordsToTextureDimensions(rawTexCoords,
				textureDimensionsForScaling, totalX, totalZ);

	}

	/**
	 * takes raw texture coordinates (for an idealized 1m x 1m texture) and scales them to fit an actual texture
	 * based on that texture's {@link TextureDataDimensions}.
	 * Also implements padding based on ({@link TextureDataDimensions#padding()}) and snapping (based on
	 * {@link TextureDataDimensions#widthPerEntity()} and based on {@link TextureDataDimensions#heightPerEntity()}).
	 *
	 * @param totalX  size of the range of X values, used for texture snapping, optional
	 * @param totalZ  size of the range of Z values, used for texture snapping, optional
	 */
	private static List<VectorXZ> scaleTexCoordsToTextureDimensions(List<VectorXZ> rawTexCoords,
			TextureDataDimensions dim, @Nullable Double totalX, @Nullable Double totalZ) {

		List<VectorXZ> result = new ArrayList<>(rawTexCoords.size());

		for (VectorXZ v : rawTexCoords) {

			double width = dim.width();
			double height = dim.height();

			if (totalX != null && dim.widthPerEntity() != null) {
				long entities = Long.max(1, round(totalX / dim.widthPerEntity()));
				double textureRepeats = entities / (dim.width() / dim.widthPerEntity());
				width = totalX / textureRepeats;
			}

			if (totalZ != null && dim.heightPerEntity() != null) {
				long entities = Long.max(1, round(totalZ / dim.heightPerEntity()));
				double textureRepeats = entities / (dim.height() / dim.heightPerEntity());
				height = totalZ / textureRepeats;
			}

			double x = v.x / width;
			double z = v.z / height;

			result.add(TexCoordUtil.applyPadding(new VectorXZ(x, z), dim));

		}

		return result;

	}

	@Override
	public List<VectorXZ> apply(List<VectorXYZ> vs) {
		if (vs.size() != texCoords.size()) {
			throw new IllegalArgumentException("incorrect number of vertices");
		}
		return texCoords;
	}

}
