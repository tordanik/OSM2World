package org.osm2world.target.gltf.data;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class GltfScene {

	public List<Integer> nodes;

	public @Nullable String name;
	public @Nullable Map<String, Object> extensions;
	public @Nullable Object extras;

}
