package org.osm2world.scene.texcoord;

import static java.util.Collections.*;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;

/**
 * a texture coordinate function that stores a known texture coordinate for each vertex.
 * Unlike {@link MapBasedTexCoordFunction}, this allows vertices in the same location to have different texture coords.
 */
public class PrecomputedTexCoordFunction implements TexCoordFunction {

	public final List<VectorXZ> texCoords;

	public PrecomputedTexCoordFunction(List<VectorXZ> texCoords) {
		this.texCoords = unmodifiableList(texCoords);
	}

	@Override
	public List<VectorXZ> apply(List<VectorXYZ> vs) {
		if (vs.size() != texCoords.size()) {
			throw new IllegalArgumentException("incorrect number of vertices");
		}
		return texCoords;
	}

	public static PrecomputedTexCoordFunction merge(List<PrecomputedTexCoordFunction> texCoordFunctions) {

		switch (texCoordFunctions.size()) {
		case 0: return new PrecomputedTexCoordFunction(emptyList());
		case 1: return texCoordFunctions.get(0);
		}

		List<VectorXZ> mergedList = new ArrayList<>();

		for (PrecomputedTexCoordFunction texCoordFunction : texCoordFunctions) {
			mergedList.addAll(texCoordFunction.texCoords);
		}

		return new PrecomputedTexCoordFunction(mergedList);

	}

}
