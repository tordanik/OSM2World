package org.osm2world.output.gltf.data;

import java.util.Map;

import javax.annotation.Nullable;

import org.teavm.flavour.json.JsonPersistable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonPersistable
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

	@JsonCreator
	public GltfAccessor(@JsonProperty(value = "componentType") int componentType,
			@JsonProperty(value = "count") int count,
			@JsonProperty(value = "type") String type) {

		if (count <= 0) { throw new IllegalArgumentException("invalid count: " + count); }

		this.componentType = componentType;
		this.count = count;
		this.type = type;

	}

}
