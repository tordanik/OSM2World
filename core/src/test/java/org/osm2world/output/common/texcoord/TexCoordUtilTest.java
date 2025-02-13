package org.osm2world.output.common.texcoord;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.osm2world.math.VectorXZ;
import org.osm2world.output.common.material.TextureDataDimensions;

public class TexCoordUtilTest {

	@Test
	public void testApplyPadding() {

		var dim = new TextureDataDimensions(1, 1, null, null, 0.1);

		assertEquals(0.1, TexCoordUtil.applyPadding(new VectorXZ(0, 0), dim).x, 1e-5);
		assertEquals(0.3, TexCoordUtil.applyPadding(new VectorXZ(0.25, 0), dim).x, 1e-5);
		assertEquals(0.5, TexCoordUtil.applyPadding(new VectorXZ(0.5, 0), dim).x, 1e-5);
		assertEquals(0.7, TexCoordUtil.applyPadding(new VectorXZ(0.75, 0), dim).x, 1e-5);
		assertEquals(0.9, TexCoordUtil.applyPadding(new VectorXZ(1.0, 0), dim).x, 1e-5);

	}

}
