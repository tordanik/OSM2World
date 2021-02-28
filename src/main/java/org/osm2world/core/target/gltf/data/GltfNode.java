package org.osm2world.core.target.gltf.data;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class GltfNode {

	public @Nullable Integer camera;
	public @Nullable List<Integer> children;
	public @Nullable Integer skin;
	public @Nullable float[] matrix;
	public @Nullable Integer mesh;
	public @Nullable float[] rotation;
	public @Nullable float[] scale;
	public @Nullable float[] translation;
	public @Nullable List<Float> weights;

	public @Nullable String name;
	public @Nullable Map<String, Object> extensions;
	public @Nullable Object extras;

}
