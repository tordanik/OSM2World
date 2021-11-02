package org.osm2world.core.target.common.texcoord;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.TextureDataDimensions;

/** a texture coordinate function that stores a known texture coordinate for each vertex */
public class PrecomputedTexCoordFunction implements TexCoordFunction {

	private final Map<VectorXYZ, VectorXZ> texCoordMap;

	/** if present, stored values are for a 1m x 1m texture and need to be scaled (e.g. halved for 2m x 2m textures) */
	private final @Nullable TextureDataDimensions textureDimensionsForScaling;

	public PrecomputedTexCoordFunction(Map<VectorXYZ, VectorXZ> texCoordMap,
			TextureDataDimensions textureDimensionsForScaling) {
		this.texCoordMap = texCoordMap;
		this.textureDimensionsForScaling = textureDimensionsForScaling;
	}

	public PrecomputedTexCoordFunction(Map<VectorXYZ, VectorXZ> texCoordMap) {
		this(texCoordMap, null);
	}

	public PrecomputedTexCoordFunction(List<VectorXYZ> vertices, List<VectorXZ> texCoords) {

		if (vertices.size() != texCoords.size()) {
			throw new IllegalArgumentException("There must be exactly one texture coordinate per vertex");
		}
		this.texCoordMap = new HashMap<>();

		for (int i = 0; i < vertices.size(); i++) {
			texCoordMap.put(vertices.get(i), texCoords.get(i));
		}

		this.textureDimensionsForScaling = null;

	}

	@Override
	public List<VectorXZ> apply(List<VectorXYZ> vs) {
		return vs.stream().map(v -> apply(v)).collect(toList());
	}

	public VectorXZ apply(VectorXYZ v) {
		VectorXZ result = texCoordMap.get(v);
		if (textureDimensionsForScaling != null) {
			result = new VectorXZ(
					result.x / textureDimensionsForScaling.width,
					result.z / textureDimensionsForScaling.height);
		}
		return result;
	}

	public static PrecomputedTexCoordFunction merge(List<PrecomputedTexCoordFunction> texCoordFunctions) {

		switch (texCoordFunctions.size()) {
		case 0: return new PrecomputedTexCoordFunction(emptyMap());
		case 1: return texCoordFunctions.get(0);
		}

		Map<VectorXYZ, VectorXZ> mergedMap = new HashMap<>();

		for (PrecomputedTexCoordFunction texCoordFunction : texCoordFunctions) {
			texCoordFunction.texCoordMap.forEach((k, v) -> mergedMap.merge(k, v, (v1, v2) -> v1));
		}

		return new PrecomputedTexCoordFunction(mergedMap );

	}

}
