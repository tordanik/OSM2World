package org.osm2world.scene.material;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.osm2world.math.VectorXZ;

public class TextureDataDimensionsTest {

	@Test
	public void testApplyPadding() {

		var dim = new TextureDataDimensions(1, 1, null, null, 0.1);

		assertEquals(0.1, dim.applyPadding(new VectorXZ(0, 0)).x, 1e-5);
		assertEquals(0.3, dim.applyPadding(new VectorXZ(0.25, 0)).x, 1e-5);
		assertEquals(0.5, dim.applyPadding(new VectorXZ(0.5, 0)).x, 1e-5);
		assertEquals(0.7, dim.applyPadding(new VectorXZ(0.75, 0)).x, 1e-5);
		assertEquals(0.9, dim.applyPadding(new VectorXZ(1.0, 0)).x, 1e-5);

	}

}