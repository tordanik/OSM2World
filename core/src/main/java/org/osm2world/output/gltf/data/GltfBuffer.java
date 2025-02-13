package org.osm2world.output.gltf.data;

import java.util.Map;

import javax.annotation.Nullable;

public class GltfBuffer {

	public @Nullable String uri;
	public final int byteLength;

	public @Nullable String name;
	public @Nullable Map<String, Object> extensions;
	public @Nullable Object extras;

	public GltfBuffer(int byteLength) {
		this.byteLength = byteLength;
	}

}
