package org.osm2world.core.target.gltf.data;

import java.util.Map;

import javax.annotation.Nullable;

public class GltfBufferView {

	public static final int TARGET_ARRAY_BUFFER = 34962;
	public static final int TARGET_ELEMENT_ARRAY_BUFFER = 34963;

	public final int buffer;
	public @Nullable Integer byteOffset;
	public final int byteLength;
	public @Nullable Integer byteStride;
	public @Nullable Integer target;

	public @Nullable String name;
	public @Nullable Map<String, Object> extensions;
	public @Nullable Object extras;

	public GltfBufferView(int buffer, int byteLength) {
		this.buffer = buffer;
		this.byteLength = byteLength;
	}

}
