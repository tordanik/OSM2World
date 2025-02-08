package org.osm2world.target.gltf.data;

import java.util.Map;

import javax.annotation.Nullable;

public class GltfImage {

	public @Nullable String uri;
	public @Nullable String mimeType;
	public @Nullable Integer bufferView;

	public @Nullable String name;
	public @Nullable Map<String, Object> extensions;
	public @Nullable Object extras;

}
