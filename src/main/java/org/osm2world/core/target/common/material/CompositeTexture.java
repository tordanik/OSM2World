package org.osm2world.core.target.common.material;

import static java.lang.Math.ceil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.jogamp.opengl.util.awt.ImageUtil;

/**
 * two textures (with compatible dimensions) combined with each other
 */
public class CompositeTexture extends RuntimeTexture {

	public enum CompositeMode {
		/** combine {@link #textureA}'s alpha channel with {@link #textureB}'s RGB channels */
		ALPHA_FROM_A,
		/** draw {@link #textureA}, then {@link #textureB} on top */
		STACKED
	}

	private final CompositeMode mode;

	private final TextureData textureA;
	private final TextureData textureB;

	public CompositeTexture(CompositeMode mode, TextureData textureA, TextureData textureB) {
		super(textureA.width, textureA.height, textureA.widthPerEntity, textureA.heightPerEntity,
				textureA.wrap, textureA.coordFunction);
		this.mode = mode;
		this.textureA = textureA;
		this.textureB = textureB;
	}

	@Override
	public BufferedImage getBufferedImage() {

		/* obtain images and dimensions */

		BufferedImage imageA = textureA.getBufferedImage();
	    BufferedImage imageB = textureB.getBufferedImage();

		int imageWidth = imageA.getWidth();
		int imageHeight = imageA.getHeight();

		/* flip the images (this is a workaround for an unknown problem - TODO: understand this better) */

	    ImageUtil.flipImageVertically(imageA);
	    ImageUtil.flipImageVertically(imageB);

	    /* repeat imageB to fill the necessary space */

	    //TODO: should take TextureData.width and .height into account and scale the image

		if (imageB.getHeight() < imageHeight || imageB.getWidth() < imageWidth) {

			BufferedImage tiledImageB = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = tiledImageB.createGraphics();

			for (int repeatX = 0; repeatX < (int)ceil(imageB.getWidth() / imageWidth); repeatX ++) {
				for (int repeatY = 0; repeatY < (int)ceil(imageHeight / imageB.getHeight()); repeatY ++) {
					g2d.drawImage(imageA, imageWidth * repeatX, imageHeight * repeatY, null);
				}
			}

			g2d.dispose();

			imageB = tiledImageB;

		}

		/* combine the images into a result */

		BufferedImage result = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);

		if (mode == CompositeMode.STACKED) {

			Graphics2D g2d = result.createGraphics();
			g2d.drawImage(imageA, 0, 0, imageA.getWidth(), imageA.getHeight(), null);
			g2d.drawImage(imageB, 0, 0, imageB.getWidth(), imageB.getHeight(), null);
			g2d.dispose();

		} else if (mode == CompositeMode.ALPHA_FROM_A) {

			for (int y = 0; y < imageHeight; y++) {
				for (int x = 0; x < imageWidth; x++) {
					Color cA = new Color(imageA.getRGB(x, y), true);
					Color cB = new Color(imageB.getRGB(x, y));
					Color colorWithAlpha = new Color(cB.getRed(), cB.getGreen(), cB.getBlue(), cA.getAlpha());
					result.setRGB(x, y, colorWithAlpha.getRGB());
				}
			}

		} else {
			throw new Error("Unsupported mode " + mode);
		}

		return result;

	}

	@Override
	public String toString() {
		return "CompositeTexture [" + mode + ", " + textureA + " + " + textureB + "]";
	}

}
