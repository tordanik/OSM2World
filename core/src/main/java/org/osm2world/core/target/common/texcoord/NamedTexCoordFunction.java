package org.osm2world.core.target.common.texcoord;

import java.util.function.Function;

import org.osm2world.core.math.shapes.FaceXYZ;
import org.osm2world.core.target.common.material.TextureDataDimensions;

/**
 * offers generators for several useful {@link TexCoordFunction} implementations.
 * They can be referenced by name in style definition files.
 */
public enum NamedTexCoordFunction implements Function<TextureDataDimensions, TexCoordFunction> {

	/**
	 * uses x and z vertex coords together with the texture's width and height
	 * to place a texture. This function works for all geometries,
	 * but steep inclines or even vertical walls produce odd-looking results.
	 */
	GLOBAL_X_Z,

	/**
	 * like {@link #GLOBAL_X_Z}, but uses y instead of z dimension.
	 * Better suited for certain vertical surfaces.
	 */
	GLOBAL_X_Y,

	/**
	 * like {@link #GLOBAL_X_Z}, but uses y instead of x dimension.
	 * Better suited for certain vertical surfaces.
	 */
	GLOBAL_Z_Y,

	/**
	 * creates texture coordinates for individual triangles that
	 * orient the texture based on each triangle's downward slope.
	 *
	 * TODO: introduce face requirement?
	 */
	SLOPED_TRIANGLES,

	/**
	 * creates texture coordinates for a triangle strip (alternating between
	 * upper and lower vertex), based on the length along a wall from the
	 * starting point, height of the vertex, and texture size.
	 *
	 * This only works for vertices forming a triangle strip,
	 * alternating between upper and lower vertex.
	 */
	STRIP_WALL,

	/**
	 * creates texture coordinates for a triangle strip (alternating between
	 * upper and lower vertex), based on the length along a wall from the
	 * starting point.
	 *
	 * Similar to {@link #STRIP_WALL}, except that one texture coordinate
	 * dimension alternates between 1 and 0 instead of being based on height.
	 */
	STRIP_FIT_HEIGHT,

	/**
	 * stretches the texture exactly once onto a triangle strip (alternating
	 * between upper and lower vertex).
	 *
	 * Most commonly used to texture a rectangle represented as a
	 * triangle strip with 2 triangles.
	 */
	STRIP_FIT,

	/**
	 * fits an image onto a flat polygon.
	 * Vertices must represent the vertex loop of a {@link FaceXYZ}
	 */
	FACE_FIT;

	@Override
	public TexCoordFunction apply(TextureDataDimensions dimensions) {
		return switch (this) {
			case GLOBAL_X_Y -> new GlobalXYTexCoordFunction(dimensions);
			case GLOBAL_X_Z -> new GlobalXZTexCoordFunction(dimensions);
			case GLOBAL_Z_Y -> new GlobalZYTexCoordFunction(dimensions);
			case SLOPED_TRIANGLES -> new SlopedTrianglesTexCoordFunction(dimensions);
			case STRIP_WALL, STRIP_FIT_HEIGHT, STRIP_FIT ->
					new StripWallTexCoordFunction(dimensions, this == STRIP_FIT, this != STRIP_WALL);
			case FACE_FIT -> new FaceFitTexCoordFunction(dimensions);
		};
	}

}
