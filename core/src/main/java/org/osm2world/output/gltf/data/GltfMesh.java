package org.osm2world.output.gltf.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class GltfMesh {

	public static final int POINTS = 0;
	public static final int LINES = 1;
	public static final int LINE_LOOP = 2;
	public static final int LINE_STRIP = 3;
	public static final int TRIANGLES = 4;
	public static final int TRIANGLE_STRIP = 5;
	public static final int TRIANGLE_FAN = 6;

	public static class Primitive {

		public Map<String, Integer> attributes = new HashMap<>();
		public @Nullable Integer indices;
		public @Nullable Integer material;
		public @Nullable Integer mode;

		public @Nullable Map<String, Object> extensions;
		public @Nullable Object extras;

	}

	public List<Primitive> primitives = new ArrayList<>();
	public @Nullable List<Float> weights;

	public @Nullable String name;
	public @Nullable Map<String, Object> extensions;
	public @Nullable Object extras;

}
