package org.osm2world.core.target.common;

import static java.awt.Color.*;
import static java.util.Arrays.asList;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;

import org.junit.Test;
import org.osm2world.core.target.common.material.RuntimeTexture;
import org.osm2world.core.target.common.material.TextureData;
import org.osm2world.core.target.common.material.TextureData.Wrap;
import org.osm2world.core.util.Resolution;
import org.osm2world.core.util.color.LColor;

public class TextureDataTest {

	@Test
	public void testAverageColor_singleColor() {

		for (Color testColor : asList(BLACK, WHITE, GREEN)) {

			TextureData testTexture = drawTestTexture((res, g2d) -> {
				g2d.setBackground(testColor);
				g2d.clearRect(0, 0, res.width, res.height);
			});

			assertAlmostEquals(testColor, testTexture.getAverageColor().toAWT());

		}

	}

	@Test
	public void testAverageColor_twoColors() {

		TextureData testTexture = drawTestTexture((res, g2d) -> {
			int widthLeft = res.width / 2;
			g2d.setBackground(RED);
			g2d.clearRect(0, 0, widthLeft, res.height);
			g2d.setBackground(BLUE);
			g2d.clearRect(widthLeft, 0, res.width - widthLeft, res.height);
		});

		System.out.println(testTexture.getAverageColor());
		assertAlmostEquals(new LColor(0.5f, 0f, 0.5f).toAWT(), testTexture.getAverageColor().toAWT());

	}

	private static final TextureData drawTestTexture(BiConsumer<Resolution, Graphics2D> drawImpl) {
		return new RuntimeTexture(1, 1, null, null, Wrap.REPEAT, GLOBAL_X_Z) {
			@Override
			public BufferedImage getBufferedImage() {
				Resolution res = new Resolution(128, 128);
				BufferedImage image = new BufferedImage(res.width, res.height, BufferedImage.TYPE_INT_ARGB);
				drawImpl.accept(res, image.createGraphics());
				return image;
			}
		};
	}

}
