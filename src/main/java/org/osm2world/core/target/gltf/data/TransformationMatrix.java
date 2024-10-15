package org.osm2world.core.target.gltf.data;

import static java.lang.Math.sqrt;

import java.util.Arrays;

import org.osm2world.core.math.VectorXYZ;

/**
 * 4x4 transformation matrix
 *
 * @param values  16 values stored in column-major order
 */
public record TransformationMatrix (float[] values) {

	public TransformationMatrix {
		if (values.length != 16) {
			throw new IllegalArgumentException("Transformation matrix must be 4x4");
		}
	}

	public float[] row(int i) {
		if (i < 0 || i > 3) throw new IllegalArgumentException("illegal row index: " + i);
		return new float[] {
				values[i*4], values[i*4+1], values[i*4+2], values[i*4+3]
		};
	}

	public float[] col(int i) {
		if (i < 0 || i > 3) throw new IllegalArgumentException("illegal column index: " + i);
		return new float[] {
				values[i], values[i+4], values[i+8], values[i+12]
		};
	}

	public float get(int col, int row) {
		return values[col*4 + row];
	}

	public TransformationMatrix transpose() {
		float[] result = new float[16];
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				result[i*4 + j] = get(j, i);
			}
		}
		return new TransformationMatrix(result);
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof TransformationMatrix that
				&& Arrays.equals(values, that.values);
	}

	@Override
	public String toString() {
		return Arrays.toString(values);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(values);
	}

	public static TransformationMatrix forTRS(float[] translation, float[] rotation, float[] scale) {
		var t = TransformationMatrix.forTranslation(translation);
		var r = TransformationMatrix.forRotation(rotation);
		var s = TransformationMatrix.forScale(scale);
		return t.times(r).times(s);
	}

	public static TransformationMatrix forTranslation(float[] translation) {
		if (translation.length != 3) throw new IllegalArgumentException();
		return new TransformationMatrix(new float[] {
				1f, 0f, 0f, translation[0],
				0f, 1f, 0f, translation[1],
				0f, 0f, 1f, translation[2],
				0f, 0f, 0f, 1f
		}).transpose();
	}

	public static TransformationMatrix forRotation(float[] rotation) {

		if (rotation.length != 4) throw new IllegalArgumentException();

		float qx = rotation[0], qy = rotation[1], qz = rotation[2], qw = rotation[3];

		// normalize the quaternion
		float norm = (float) sqrt(qx*qx + qy*qy + qz*qz + qw*qw);
		qx /= norm;
		qy /= norm;
		qz /= norm;
		qw /= norm;

		return new TransformationMatrix(new float[] {
			1.0f - 2.0f*qy*qy - 2.0f*qz*qz, 2.0f*qx*qy - 2.0f*qz*qw, 2.0f*qx*qz + 2.0f*qy*qw, 0.0f,
			2.0f*qx*qy + 2.0f*qz*qw, 1.0f - 2.0f*qx*qx - 2.0f*qz*qz, 2.0f*qy*qz - 2.0f*qx*qw, 0.0f,
			2.0f*qx*qz - 2.0f*qy*qw, 2.0f*qy*qz + 2.0f*qx*qw, 1.0f - 2.0f*qx*qx - 2.0f*qy*qy, 0.0f,
			0.0f, 0.0f, 0.0f, 1.0f
		}).transpose();

	}

	public static TransformationMatrix forScale(float[] scale) {
		if (scale.length != 3) throw new IllegalArgumentException();
		return new TransformationMatrix(new float[] {
				scale[0], 0f, 0f, 0f,
				0f, scale[1], 0f, 0f,
				0f, 0f, scale[2], 0f,
				0f, 0f, 0f, 1f
		});
	}

	public TransformationMatrix times(TransformationMatrix m) {
		float[] result = new float[16];
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				for (int k = 0; k < 4; k++) {
					result[col * 4 + row] += this.get(k, row) * m.get(col, k);
				}
			}
		}
		return new TransformationMatrix(result);
	}

	public VectorXYZ applyTo(VectorXYZ v) {

		var x = get(0, 0) * v.x + get(1, 0) * v.y + get(2, 0) * v.z + get(3, 0) * 1;
		var y = get(0, 1) * v.x + get(1, 1) * v.y + get(2, 1) * v.z + get(3, 1) * 1;
		var z = get(0, 2) * v.x + get(1, 2) * v.y + get(2, 2) * v.z + get(3, 2) * 1;
		var w = get(0, 3) * v.x + get(1, 3) * v.y + get(2, 3) * v.z + get(3, 3) * 1;

		return new VectorXYZ(x, y, z);

	}

}
