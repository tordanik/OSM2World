package org.osm2world.core.target.gltf.data;

import java.util.Map;

import javax.annotation.Nullable;

public class GltfSampler {

	//TODO more constants, turn into enums with int field
	public static final int WRAP_CLAMP_TO_EDGE = 33071;
	public static final int WRAP_MIRRORED_REPEAT = 33648;
	public static final int WRAP_REPEAT = 10497;

	public @Nullable Integer minFilter;
	public @Nullable Integer magFilter;
	public @Nullable Integer wrapS;
	public @Nullable Integer wrapT;

	public @Nullable String name;
	public @Nullable Map<String, Object> extensions;
	public @Nullable Object extras;

}
