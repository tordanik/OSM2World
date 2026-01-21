package org.osm2world.output.gltf.data;

import java.util.Map;

import javax.annotation.Nullable;

import org.teavm.flavour.json.JsonPersistable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonPersistable
public class GltfBuffer {

	public @Nullable String uri;
	public final int byteLength;

	public @Nullable String name;
	public @Nullable Map<String, Object> extensions;
	public @Nullable Object extras;

	@JsonCreator
	public GltfBuffer(@JsonProperty(value = "byteLength") int byteLength) {
		this.byteLength = byteLength;
	}

}
