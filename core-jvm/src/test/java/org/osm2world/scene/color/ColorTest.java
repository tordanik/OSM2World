package org.osm2world.scene.color;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ColorTest {

	@Test
	public void testGetColorComponents() {

		Color colorRGB = new Color(0, 128, 255);

		assertEquals(0, colorRGB.getRed());
		assertEquals(128, colorRGB.getGreen());
		assertEquals(255, colorRGB.getBlue());
		assertEquals(255, colorRGB.getAlpha());

		assertEquals(0f, colorRGB.getColorComponents(null)[0], 0.001f);
		assertEquals(0.502f, colorRGB.getColorComponents(null)[1], 0.001f);
		assertEquals(1.0f, colorRGB.getColorComponents(null)[2], 0.001f);

		Color colorRGBA = new Color(0.2f, 0.4f, 0.6f, 0.5f);

		assertEquals(0.2f, colorRGBA.getColorComponents(new float[3])[0], 0.00f);
		assertEquals(0.4f, colorRGBA.getColorComponents(new float[3])[1], 0.00f);
		assertEquals(0.6f, colorRGBA.getColorComponents(new float[3])[2], 0.00f);

	}


}