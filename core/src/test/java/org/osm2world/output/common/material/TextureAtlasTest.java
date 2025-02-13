package org.osm2world.output.common.material;

import static java.awt.Color.*;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.osm2world.output.common.material.TextureTestUtil.drawSingleColorTexture;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.osm2world.math.VectorXZ;

public class TextureAtlasTest {

	@Test
	public void testGetBufferedImage() throws IOException {

		List<TextureData> testTextures = asList(
				drawSingleColorTexture(RED),
				drawSingleColorTexture(YELLOW),
				drawSingleColorTexture(WHITE),
				drawSingleColorTexture(BLACK));

		TextureAtlas atlas = new TextureAtlas(testTextures);
		BufferedImage atlasImage = atlas.getBufferedImage();

		assertEquals(RED, new Color(atlasImage.getRGB(1 * atlasImage.getWidth() / 4, 1 * atlasImage.getHeight() / 4)));
		assertEquals(YELLOW, new Color(atlasImage.getRGB(3 * atlasImage.getWidth() / 4, 1 * atlasImage.getHeight() / 4)));
		assertEquals(WHITE, new Color(atlasImage.getRGB(1 * atlasImage.getWidth() / 4, 3 * atlasImage.getHeight() / 4)));
		assertEquals(BLACK, new Color(atlasImage.getRGB(3 * atlasImage.getWidth() / 4, 3 * atlasImage.getHeight() / 4)));

		assertAlmostEquals(0, 0.5, atlas.mapTexCoord(testTextures.get(0), new VectorXZ(0, 0)));
		assertAlmostEquals(0.25, 1.0, atlas.mapTexCoord(testTextures.get(0), new VectorXZ(0.5, 1.0)));
		assertAlmostEquals(0.25, 0.5, atlas.mapTexCoord(testTextures.get(2), new VectorXZ(0.5, 1.0)));

	}

}
