package org.osm2world.core.target.common.material;

import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

/**
 * utility class for texture coordinate calculation
 */
public final class TexCoordUtil {

	/** prevents instantiation */
	private TexCoordUtil() {}

	/**
	 * calculates the texture coordinate lists based on the
	 * {@link TexCoordFunction} associated with each {@link TextureLayer}
	 */
	public static final List<List<VectorXZ>> texCoordLists(
			List<VectorXYZ> vs, Material material,
			TexCoordFunction defaultCoordFunction) {

		List<TextureLayer> textureLayers = material.getTextureLayers();

		if (textureLayers.size() == 0) {

			return emptyList();

		} else if (textureLayers.size() == 1) {

			TextureData textureData = textureLayers.get(0).baseColorTexture;
			TexCoordFunction coordFunction = textureData.coordFunction;
			if (coordFunction == null) { coordFunction = defaultCoordFunction; }

			return singletonList(coordFunction.apply(vs, textureData));

		} else {

			List<List<VectorXZ>> result = new ArrayList<>();

			for (TextureLayer textureLayer : textureLayers) {

				TextureData textureData = textureLayer.baseColorTexture;

				TexCoordFunction coordFunction = textureData.coordFunction;
				if (coordFunction == null) { coordFunction = defaultCoordFunction; }

				result.add(coordFunction.apply(vs, textureData));

			}

			return result;

		}

	}

	/**
	 * equivalent of {@link #texCoordLists(List, Material, TexCoordFunction)}
	 * for a collection of triangle objects.
	 */
	public static final List<List<VectorXZ>> triangleTexCoordLists(
			Collection<TriangleXYZ> triangles, Material material,
			TexCoordFunction defaultCoordFunction) {

		List<VectorXYZ> vs = new ArrayList<VectorXYZ>(triangles.size() * 3);

		for (TriangleXYZ triangle : triangles) {
			vs.add(triangle.v1);
			vs.add(triangle.v2);
			vs.add(triangle.v3);
		}

		return texCoordLists(vs, material, defaultCoordFunction);

	}

}
