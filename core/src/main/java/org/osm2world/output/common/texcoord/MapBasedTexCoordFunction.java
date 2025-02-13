package org.osm2world.output.common.texcoord;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.output.common.material.TextureDataDimensions;

/** a texture coordinate function that stores a known texture coordinate for each 3d position */
public class MapBasedTexCoordFunction implements TexCoordFunction {

	private final Map<VectorXYZ, VectorXZ> texCoordMap;

	/** if present, stored values are for a 1m x 1m texture and need to be scaled (e.g. halved for 2m x 2m textures) */
	private final @Nullable TextureDataDimensions textureDimensionsForScaling;

	public MapBasedTexCoordFunction(Map<VectorXYZ, VectorXZ> texCoordMap,
			TextureDataDimensions textureDimensionsForScaling) {
		this.texCoordMap = texCoordMap;
		this.textureDimensionsForScaling = textureDimensionsForScaling;
	}

	public MapBasedTexCoordFunction(Map<VectorXYZ, VectorXZ> texCoordMap) {
		this(texCoordMap, null);
	}

	@Override
	public List<VectorXZ> apply(List<VectorXYZ> vs) {
		return vs.stream().map(v -> apply(v)).collect(toList());
	}

	public VectorXZ apply(VectorXYZ v) {
		VectorXZ result = texCoordMap.get(v);
		if (textureDimensionsForScaling != null) {
			result = TexCoordUtil.applyPadding(new VectorXZ(
						result.x / textureDimensionsForScaling.width(),
						result.z / textureDimensionsForScaling.height()),
					textureDimensionsForScaling);
		}
		return result;
	}

	public static MapBasedTexCoordFunction merge(List<MapBasedTexCoordFunction> texCoordFunctions) {

		switch (texCoordFunctions.size()) {
		case 0: return new MapBasedTexCoordFunction(emptyMap());
		case 1: return texCoordFunctions.get(0);
		}

		Map<VectorXYZ, VectorXZ> mergedMap = new HashMap<>();

		for (MapBasedTexCoordFunction texCoordFunction : texCoordFunctions) {
			texCoordFunction.texCoordMap.forEach((k, v) -> mergedMap.merge(k, v, (v1, v2) -> v1));
		}

		return new MapBasedTexCoordFunction(mergedMap );

	}

}
