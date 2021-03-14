package org.osm2world.core.target.common.material;

import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

/** a texture coordinate function that stores a known texture coordinate for each vertex */
public class PrecomputedTexCoordFunction implements TexCoordFunction {

	private final Map<VectorXYZ, VectorXZ> texCoordMap;

	/** if true, the stored values are for a 1m x 1m texture and need to be scaled (e.g. halved for 2m x 2m textures) */
	private final boolean needsScalingToTexture;

	public PrecomputedTexCoordFunction(Map<VectorXYZ, VectorXZ> texCoordMap, boolean needsScalingToTexture) {
		this.texCoordMap = texCoordMap;
		this.needsScalingToTexture = needsScalingToTexture;
	}

	public PrecomputedTexCoordFunction(Map<VectorXYZ, VectorXZ> texCoordMap) {
		this(texCoordMap, false);
	}

	public PrecomputedTexCoordFunction(List<VectorXYZ> vertices, List<VectorXZ> texCoords) {

		if (vertices.size() != texCoords.size()) {
			throw new IllegalArgumentException("There must be exactly one texture coordinate per vertex");
		}
		this.texCoordMap = new HashMap<>();

		for (int i = 0; i < vertices.size(); i++) {
			texCoordMap.put(vertices.get(i), texCoords.get(i));
		}

		this.needsScalingToTexture = false;

	}

	@Override
	public List<VectorXZ> apply(List<VectorXYZ> vs, TextureData textureData) {
		return vs.stream().map(v -> apply(v, textureData)).collect(toList());
	}

	public VectorXZ apply(VectorXYZ v, TextureData textureData) {
		VectorXZ result = texCoordMap.get(v);
		if (needsScalingToTexture) {
			result = new VectorXZ(result.x / textureData.width, result.z / textureData.height);
		}
		return result;
	}

}
