package org.osm2world.target.gltf.data;

import java.util.Map;

import javax.annotation.Nullable;

public class GltfAccessor {

	public static final int TYPE_BYTE = 5120;
	public static final int TYPE_UNSIGNED_BYTE = 5121;
	public static final int TYPE_SHORT = 5122;
	public static final int TYPE_UNSIGNED_SHORT = 5123;
	public static final int TYPE_UNSIGNED_INT = 5125;
	public static final int TYPE_FLOAT = 5126;

	public @Nullable Integer bufferView;
	public @Nullable Integer byteOffset;
	public final int componentType;
	public @Nullable Boolean normalized;
	public final int count;
	public final String type;
	public @Nullable float[] max;
	public @Nullable float[] min;
	public @Nullable Object sparse;

	public @Nullable String name;
	public @Nullable Map<String, Object> extensions;
	public @Nullable Object extras;

	public GltfAccessor(int componentType, int count, String type) {

		if (count <= 0) { throw new IllegalArgumentException("invalid count: " + count); }

		this.componentType = componentType;
		this.count = count;
		this.type = type;

	}

}
