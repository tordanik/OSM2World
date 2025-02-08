package org.osm2world.target.gltf.data;

import java.util.Map;

import javax.annotation.Nullable;

public class GltfMaterial {

	public static class TextureInfo {

		public int index;
		public @Nullable Integer texCoord;

		public @Nullable Map<String, Object> extensions;
		public @Nullable Object extras;

	}

	public static class NormalTextureInfo extends TextureInfo {
		public @Nullable Float scale;
	}

	public static class OcclusionTextureInfo extends TextureInfo {
		public @Nullable Float strength;
	}

	public static class PbrMetallicRoughness {

		public @Nullable float[] baseColorFactor;
		public @Nullable TextureInfo baseColorTexture;
		public @Nullable Float metallicFactor;
		public @Nullable Float roughnessFactor;
		public @Nullable TextureInfo metallicRoughnessTexture;

		public @Nullable Map<String, Object> extensions;
		public @Nullable Object extras;

	}

	public @Nullable PbrMetallicRoughness pbrMetallicRoughness;
	public @Nullable NormalTextureInfo normalTexture;
	public @Nullable OcclusionTextureInfo occlusionTexture;
	public @Nullable TextureInfo emissiveTexture;
	public @Nullable float[] emissiveFactor;
	public @Nullable String alphaMode;
	public @Nullable Float alphaCutoff;
	public @Nullable Boolean doubleSided;

	public @Nullable String name;
	public @Nullable Map<String, Object> extensions;
	public @Nullable Object extras;

}
