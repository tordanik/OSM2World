package org.osm2world.core.target.common.material;

import java.awt.image.BufferedImage;

import javax.annotation.Nullable;


public class BlankTexture extends RuntimeTexture {

	public BlankTexture(double width, double height, @Nullable Double widthPerEntity, @Nullable Double heightPerEntity) {
		super(width, height, widthPerEntity, heightPerEntity, Wrap.REPEAT, NamedTexCoordFunction.GLOBAL_X_Z);
	}

	public BlankTexture() {
		this(1.0, 1.0, null, null);
	}

	@Override
	public BufferedImage getBufferedImage() {
		return new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
	}

}
