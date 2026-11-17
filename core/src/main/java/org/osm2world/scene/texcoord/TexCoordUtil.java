package org.osm2world.scene.texcoord;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.TextureData;
import org.osm2world.scene.material.TextureDataDimensions;
import org.osm2world.scene.material.TextureLayer;

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
	public static List<TexCoordSimpleFunction> texCoordFunctions(Material material,
			Function<TextureDataDimensions, ? extends TexCoordSimpleFunction> defaultCoordFunction) {

		List<TextureLayer> textureLayers = material.textureLayers();

		if (textureLayers.isEmpty()) {

			return emptyList();

		} else if (textureLayers.size() == 1) {

			TextureData textureData = textureLayers.get(0).baseColorTexture;
			TexCoordSimpleFunction coordFunction = textureData.coordFunction;
			if (coordFunction == null) {
				coordFunction = defaultCoordFunction.apply(textureData.dimensions());
			}

			return singletonList(coordFunction);

		} else {

			List<TexCoordSimpleFunction> result = new ArrayList<>();

			for (TextureLayer textureLayer : textureLayers) {

				TextureData textureData = textureLayer.baseColorTexture;

				TexCoordSimpleFunction coordFunction = textureData.coordFunction;
				if (coordFunction == null) {
					coordFunction = defaultCoordFunction.apply(textureData.dimensions());
				}

				result.add(coordFunction);

			}

			return result;

		}

	}

	/**
	 * calculates the texture coordinate lists based on the
	 * {@link TexCoordFunction} associated with each {@link TextureLayer} and a default
	 */
	public static List<List<VectorXZ>> texCoordLists(List<VectorXYZ> vs, Material material,
			Function<TextureDataDimensions, ? extends TexCoordSimpleFunction> defaultCoordFunction) {

		List<TexCoordSimpleFunction> texCoordFunctions = texCoordFunctions(material, defaultCoordFunction);

		return switch (texCoordFunctions.size()) {
			case 0 -> emptyList();
			case 1 -> List.of(texCoordFunctions.get(0).apply(vs));
			default -> texCoordFunctions.stream().map(it -> it.apply(vs)).toList();
		};

	}

	/**
	 * equivalent of {@link #texCoordLists(List, Material, Function)}
	 * for a collection of triangle objects.
	 */
	public static List<List<VectorXZ>> triangleTexCoordLists(
			Collection<TriangleXYZ> triangles, Material material,
			Function<TextureDataDimensions, ? extends TexCoordSimpleFunction> defaultCoordFunction) {

		List<VectorXYZ> vs = new ArrayList<>(triangles.size() * 3);

		for (TriangleXYZ triangle : triangles) {
			vs.add(triangle.v1);
			vs.add(triangle.v2);
			vs.add(triangle.v3);
		}

		return texCoordLists(vs, material, defaultCoordFunction);

	}

	public static List<List<VectorXZ>> mirroredVertically(List<List<VectorXZ>> texCoordLists) {
		return texCoordLists.stream().map(list ->
				list.stream().map(v -> new VectorXZ(v.x, 1.0 - v.z)).toList()).toList();
	}

	/** returns a horizontally flipped version of a {@link TexCoordSimpleFunction} */
	public static TexCoordSimpleFunction mirroredHorizontally(TexCoordSimpleFunction texCoordFunction) {
		return (List<VectorXYZ> vs) -> {
			List<VectorXZ> result = texCoordFunction.apply(vs);
			return result.stream().map(v -> new VectorXZ(1 - v.x, v.z)).toList();
		};
	}

	/** returns a horizontally flipped version of a {@link TexCoordFunction} */
	public static TexCoordFunction mirroredHorizontally(
			Function<TextureDataDimensions, ? extends TexCoordSimpleFunction> texCoordFunction) {
		return (TextureDataDimensions dimensions, List<VectorXYZ> vs) ->
				mirroredHorizontally(texCoordFunction.apply(dimensions)).apply(vs);
	}

}
