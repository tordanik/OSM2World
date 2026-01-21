package org.osm2world.output.gltf.data;

import java.util.Map;

import javax.annotation.Nullable;

import org.teavm.flavour.json.JsonPersistable;

@JsonPersistable
public class GltfTexture {

	public @Nullable Integer sampler;
	public @Nullable Integer source;

	public @Nullable String name;
	public @Nullable Map<String, Object> extensions;
	public @Nullable Object extras;

}
