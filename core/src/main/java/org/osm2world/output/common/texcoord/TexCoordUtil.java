package org.osm2world.output.common.texcoord;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.output.common.material.Material;
import org.osm2world.output.common.material.TextureData;
import org.osm2world.output.common.material.TextureDataDimensions;
import org.osm2world.output.common.material.TextureLayer;

/**
 * utility class for texture coordinate calculation
 */
public final class TexCoordUtil {

	/** prevents instantiation */
	private TexCoordUtil() {}

	/**
	 * returns the texture coordinate functions based on the
	 * {@link TexCoordFunction} associated with each {@link TextureLayer} and a default
	 */
	public static final List<TexCoordFunction> texCoordFunctions(Material material,
			Function<TextureDataDimensions, ? extends TexCoordFunction> defaultCoordFunctionGenerator) {

		List<TextureLayer> textureLayers = material.getTextureLayers();

		if (textureLayers.size() == 0) {

			return emptyList();

		} else if (textureLayers.size() == 1) {

			TextureData textureData = textureLayers.get(0).baseColorTexture;
			TexCoordFunction coordFunction = textureData.coordFunction;
			if (coordFunction == null) {
				coordFunction = defaultCoordFunctionGenerator.apply(textureData.dimensions());
			}

			return singletonList(coordFunction);

		} else {

			List<TexCoordFunction> result = new ArrayList<>();

			for (TextureLayer textureLayer : textureLayers) {

				TextureData textureData = textureLayer.baseColorTexture;

				TexCoordFunction coordFunction = textureData.coordFunction;
				if (coordFunction == null) {
					coordFunction = defaultCoordFunctionGenerator.apply(textureData.dimensions());
				}

				result.add(coordFunction);

			}

			return result;

		}

	}

	/**
	 * calculates the texture coordinate lists based on the
	 * {@link TexCoordFunction} associated with each {@link TextureLayer}
	 */
	public static final List<List<VectorXZ>> texCoordLists(List<VectorXYZ> vs, Material material,
			Function<TextureDataDimensions, ? extends TexCoordFunction> defaultCoordFunctionGenerator) {

		List<TexCoordFunction> texCoordFunctions = texCoordFunctions(material, defaultCoordFunctionGenerator);

		switch (texCoordFunctions.size()) {
		case 0: return emptyList();
		case 1: return singletonList(texCoordFunctions.get(0).apply(vs));
		default: return texCoordFunctions.stream().map(it -> it.apply(vs)).collect(toList());
		}

	}

	/**
	 * equivalent of {@link #texCoordLists(List, Material, Function)}
	 * for a collection of triangle objects.
	 */
	public static final List<List<VectorXZ>> triangleTexCoordLists(
			Collection<TriangleXYZ> triangles, Material material,
			Function<TextureDataDimensions, ? extends TexCoordFunction> defaultCoordFunctionGenerator) {

		List<VectorXYZ> vs = new ArrayList<VectorXYZ>(triangles.size() * 3);

		for (TriangleXYZ triangle : triangles) {
			vs.add(triangle.v1);
			vs.add(triangle.v2);
			vs.add(triangle.v3);
		}

		return texCoordLists(vs, material, defaultCoordFunctionGenerator);

	}

	public static final List<List<VectorXZ>> mirroredVertically(List<List<VectorXZ>> texCoordLists) {
		return texCoordLists.stream().map(list ->
				list.stream().map(v -> new VectorXZ(v.x, 1.0 - v.z)).toList()).toList();
	}

	/** returns a horizontally flipped version of a {@link TexCoordFunction} */
	public static final TexCoordFunction mirroredHorizontally(TexCoordFunction texCoordFunction) {
		return (List<VectorXYZ> vs) -> {
			List<VectorXZ> result = texCoordFunction.apply(vs);
			return result.stream().map(v -> new VectorXZ(1 - v.x, v.z)).toList();
		};
	}

	public static final Function<TextureDataDimensions, TexCoordFunction> mirroredHorizontally(
			Function<TextureDataDimensions, ? extends TexCoordFunction> generator) {
		return (TextureDataDimensions textureDimensions) -> mirroredHorizontally(generator.apply(textureDimensions));
	}

	/**
	 * modifies a calculated texture coordinate to account for {@link TextureDataDimensions#padding()}.
	 * This is helpful when implementing {@link TexCoordFunction}s, not when using them.
	 */
	public static VectorXZ applyPadding(VectorXZ texCoord, TextureDataDimensions dimensions) {
		double padding = dimensions.padding();
		if (padding == 0) {
			return texCoord;
		} else {
			return new VectorXZ(
					padding + texCoord.x * (1 - 2 * padding),
					padding + texCoord.z * (1 - 2 * padding)
			);
		}
	}

}
