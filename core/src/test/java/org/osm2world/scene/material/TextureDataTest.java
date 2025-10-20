package org.osm2world.scene.material;

import static java.util.Arrays.asList;
import static org.osm2world.scene.color.Color.*;
import static org.osm2world.scene.material.TextureData.Wrap.CLAMP;
import static org.osm2world.scene.material.TextureData.Wrap.REPEAT;
import static org.osm2world.scene.material.TextureTestUtil.*;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import org.junit.Test;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.color.LColor;

public class TextureDataTest {

	@Test
	public void testWrap() {

		assertAlmostEquals(0.0, REPEAT.apply(0.0));
		assertAlmostEquals(0.8, REPEAT.apply(0.8));

		assertAlmostEquals(0.0, REPEAT.apply(1.0));
		assertAlmostEquals(0.5, REPEAT.apply(1.5));
		assertAlmostEquals(0.5, REPEAT.apply(3.5));

		assertAlmostEquals(0.2, REPEAT.apply(-0.8));
		assertAlmostEquals(0.2, REPEAT.apply(-10.8));

		assertAlmostEquals(0.0, CLAMP.apply(0.0));
		assertAlmostEquals(0.8, CLAMP.apply(0.8));

		assertAlmostEquals(1.0, CLAMP.apply(1.0));
		assertAlmostEquals(1.0, CLAMP.apply(1.5));
		assertAlmostEquals(1.0, CLAMP.apply(3.5));

		assertAlmostEquals(0.0, CLAMP.apply(-0.8));
		assertAlmostEquals(0.0, CLAMP.apply(-10.8));

	}

	@Test
	public void testAverageColor_singleColor() {

		for (Color testColor : asList(BLACK, WHITE, GREEN)) {
			TextureData testTexture = drawSingleColorTexture(testColor);
			assertAlmostEquals(testColor, testTexture.getAverageColor().toRGB());
		}

	}

	@Test
	public void testAverageColor_twoColors() {

		TextureData testTexture = drawTestTexture((res, g2d) -> {
			int widthLeft = res.width / 2;
			g2d.setBackground(toAWT(RED));
			g2d.clearRect(0, 0, widthLeft, res.height);
			g2d.setBackground(toAWT(BLUE));
			g2d.clearRect(widthLeft, 0, res.width - widthLeft, res.height);
		});

		assertAlmostEquals(new LColor(0.5f, 0f, 0.5f).toRGB(), testTexture.getAverageColor().toRGB());

	}

}
