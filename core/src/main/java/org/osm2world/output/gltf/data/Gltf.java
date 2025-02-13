package org.osm2world.output.gltf.data;

import java.util.List;

import javax.annotation.Nullable;

/**
 * a glTF asset. This is a simple mutable "struct" holding all data.
 * Refer to the spec at https://github.com/KhronosGroup/glTF/blob/master/specification/2.0/README.md for documentation.
 */
public class Gltf {

	public GltfAsset asset;
	public @Nullable List<String> extensionsUsed;
	public @Nullable List<String> extensionsRequired;
	public @Nullable List<GltfAccessor> accessors;
	public @Nullable List<GltfAnimation> animations;
	public @Nullable List<GltfBuffer> buffers;
	public @Nullable List<GltfBufferView> bufferViews;
	public @Nullable List<GltfCamera> cameras;
	public @Nullable List<GltfImage> images;
	public @Nullable List<GltfMaterial> materials;
	public @Nullable List<GltfMesh> meshes;
	public @Nullable List<GltfNode> nodes;
	public @Nullable List<GltfSampler> samplers;
	public @Nullable Integer scene;
	public @Nullable List<GltfScene> scenes;
	public @Nullable List<GltfSkin> skins;
	public @Nullable List<GltfTexture> textures;

}
