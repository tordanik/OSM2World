package org.osm2world.core.target.gltf.data;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TransformationMatrixTest {

	/*
	 * static fields for tests using an example from
	 * https://github.com/KhronosGroup/glTF-Tutorials/blob/main/gltfTutorial/gltfTutorial_004_ScenesNodes.md
	 */

	private static final TransformationMatrix T = new TransformationMatrix(new float[] {
			1.0f, 0.0f, 0.0f, 10.0f,
			0.0f, 1.0f, 0.0f, 20.0f,
			0.0f, 0.0f, 1.0f, 30.0f,
			0.0f, 0.0f, 0.0f, 1.0f
	}).transpose();

	private static final TransformationMatrix R = new TransformationMatrix(new float[] {
			1.0f, 0.0f, 0.0f, 0.0f,
			0.0f, 0.866f, -0.5f, 0.0f,
			0.0f, 0.5f, 0.866f, 0.0f,
			0.0f, 0.0f, 0.0f, 1.0f
	}).transpose();

	private static final TransformationMatrix S = new TransformationMatrix(new float[] {
			2.0f, 0.0f, 0.0f, 0.0f,
			0.0f, 1.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.5f, 0.0f,
			0.0f, 0.0f, 0.0f, 1.0f
	}).transpose();

	private static final TransformationMatrix TRS = new TransformationMatrix(new float[] {
			2.0f, 0.0f, 0.0f, 10.0f,
			0.0f, 0.866f, -0.25f, 20.0f,
			0.0f, 0.5f, 0.433f, 30.0f,
			0.0f, 0.0f, 0.0f, 1.0f
	}).transpose();

	@Test
	public void testTranspose() {

		var m = new TransformationMatrix(new float[] {
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
		});

		var expected = new TransformationMatrix(new float[] {
			0, 4, 8, 12, 1, 5, 9, 13, 2, 6, 10, 14, 3, 7, 11, 15
		});

		var result = m.transpose();

		assertEquals(expected, result);
		assertEquals(m, result.transpose());

	}

	@Test
	public void testTimes() {

		var m1 = new TransformationMatrix(new float[] {
			0, 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 3, 0, 0, 0
		});

		var m2 = new TransformationMatrix(new float[] {
			0, 1, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
		});

		var expected = new TransformationMatrix(new float[] {
			1 + 4 + 9, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
		});

		assertEquals(expected, m1.times(m2));

	}

	@Test
	public void testTimes_TRS() {
		assertEquals(TRS, T.times(R).times(S));
	}

	@Test
	public void testForTranslation() {
		assertEquals(T, TransformationMatrix.forTranslation(new float[] {10, 20, 30}));
	}

	@Test
	public void testForRotation() {
		TransformationMatrix r = TransformationMatrix.forRotation(new float[]{0.259f, 0.0f, 0.0f, 0.966f});
		assertArrayEquals(R.values(), r.values(), 0.001f);
	}

	@Test
	public void testForScale() {
		assertEquals(S, TransformationMatrix.forScale(new float[] {2, 1, 0.5f}));
	}

}