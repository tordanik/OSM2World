package org.osm2world.core.target.common.material;

import java.awt.image.BufferedImage;

import javax.annotation.Nullable;

import org.osm2world.core.target.common.texcoord.NamedTexCoordFunction;
import org.osm2world.core.util.Resolution;


public class BlankTexture extends RuntimeTexture {

	public BlankTexture(double width, double height, @Nullable Double widthPerEntity, @Nullable Double heightPerEntity) {
		super(width, height, widthPerEntity, heightPerEntity, Wrap.REPEAT, NamedTexCoordFunction.GLOBAL_X_Z);
	}

	public BlankTexture() {
		this(1.0, 1.0, null, null);
	}

	@Override
	public BufferedImage getBufferedImage(Resolution resolution) {
		return new BufferedImage(resolution.width, resolution.height, BufferedImage.TYPE_INT_RGB);
	}

	@Override
	public BufferedImage getBufferedImage() {
		return getBufferedImage(new Resolution(128, 128));
	}

}
