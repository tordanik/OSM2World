package org.osm2world.core.target.common.material;

import java.awt.image.BufferedImage;

import javax.annotation.Nullable;

import org.osm2world.core.target.common.texcoord.NamedTexCoordFunction;
import org.osm2world.core.util.Resolution;


public final class BlankTexture extends RuntimeTexture {

	public static final BlankTexture INSTANCE = new BlankTexture();

	private BlankTexture(double width, double height, @Nullable Double widthPerEntity, @Nullable Double heightPerEntity) {
		super(width, height, widthPerEntity, heightPerEntity, Wrap.REPEAT, NamedTexCoordFunction.GLOBAL_X_Z);
	}

	private BlankTexture() {
		this(1.0, 1.0, null, null);
	}

	@Override
	protected BufferedImage createBufferedImage(Resolution resolution) {
		return new BufferedImage(resolution.width, resolution.height, BufferedImage.TYPE_INT_RGB);
	}

	@Override
	protected BufferedImage createBufferedImage() {
		return getBufferedImage(new Resolution(128, 128));
	}

	@Override
	public String toString() {
		return "Blank";
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this;
	}

	@Override
	public int hashCode() {
		return 42;
	}

}
