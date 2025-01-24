package org.osm2world.core.target.gltf.data;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class GltfNode {

	public @Nullable Integer camera;
	public @Nullable List<Integer> children;
	public @Nullable Integer skin;
	public @Nullable float[] matrix;
	public @Nullable Integer mesh;
	public @Nullable float[] rotation;
	public @Nullable float[] scale;
	public @Nullable float[] translation;
	public @Nullable List<Float> weights;

	public @Nullable String name;
	public @Nullable Map<String, Object> extensions;
	public @Nullable Object extras;

	public TransformationMatrix getLocalTransform() {

		if (matrix != null) {
			return new TransformationMatrix(matrix);
		} else {

			float[] translation = this.translation != null ? this.translation : new float[] {0, 0, 0};
			float[] rotation = this.rotation != null ? this.rotation : new float[] {0, 0, 0, 1};
			float[] scale = this.scale != null ? this.scale : new float[] {1, 1, 1};

			return TransformationMatrix.forTRS(translation, rotation, scale);

		}

	}

}
