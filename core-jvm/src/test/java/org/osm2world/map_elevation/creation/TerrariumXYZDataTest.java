package org.osm2world.map_elevation.creation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.osm2world.scene.color.Color;

public class TerrariumXYZDataTest {

	@Test
	public void testDecodeValue() {
		assertEquals(2523.265625, TerrariumXYZData.decodeValue(new Color(137, 219, 68).getRGB()), 0.0);
	}

}
