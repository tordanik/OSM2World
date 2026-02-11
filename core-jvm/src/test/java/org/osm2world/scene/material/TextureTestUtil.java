package org.osm2world.scene.material;

import static org.osm2world.scene.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;

import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.TextureData.Wrap;
import org.osm2world.util.Resolution;

final class TextureTestUtil {

	/** prevents instantiation */
	private TextureTestUtil() { }

	static TextureData drawTestTexture(BiConsumer<Resolution, Graphics2D> drawImpl) {
		return new RuntimeTexture(new TextureDataDimensions(1, 1), Wrap.REPEAT, GLOBAL_X_Z) {
			@Override
			public BufferedImage createBufferedImage() {
				Resolution res = new Resolution(128, 128);
				BufferedImage image = new BufferedImage(res.width, res.height, BufferedImage.TYPE_INT_ARGB);
				drawImpl.accept(res, image.createGraphics());
				return image;
			}
		};
	}

	static TextureData drawSingleColorTexture(Color color) {
		return drawTestTexture((res, g2d) -> {
			g2d.setBackground(toAWT(color));
			g2d.clearRect(0, 0, res.width, res.height);
		});
	}

	static java.awt.Color toAWT(Color color) {
		return new java.awt.Color(color.getRGB());
	}


}
