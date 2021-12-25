package org.osm2world.core.target.common.material;

import static java.lang.Math.*;
import static java.util.stream.Collectors.toList;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.texcoord.TexCoordFunction;
import org.osm2world.core.util.Resolution;

/**
 * a texture atlas or spritesheet that combines multiple textures in a single image.
 * The intended texture is "selected" by using modified texture coordinates.
 * For some use cases, this has performance benefits such as reducing the number of draw calls.
 *
 * This approach only works for textures where the (original) texture coordinates are limited to the range [0,1].
 * It's not suitable for repeating textures.
 */
public class TextureAtlas extends RuntimeTexture {

	/** size of each individual texture on the atlas */
	private static final Resolution TEXTURE_RESOLUTION = new Resolution(512, 512);

	private final List<TextureData> textures;

	private final int numTexturesX;
	private final int numTexturesZ;

	public TextureAtlas(List<TextureData> textures) {

		super(1, 1, null, null, Wrap.CLAMP, null);

		this.textures = textures;

		this.numTexturesX = (int) ceil(sqrt(textures.size()));
		this.numTexturesZ = (int) ceil(textures.size() / (double)numTexturesX);

	}

	@Override
	public BufferedImage getBufferedImage() {

		BufferedImage result = new BufferedImage(numTexturesX * TEXTURE_RESOLUTION.width,
				numTexturesZ * TEXTURE_RESOLUTION.height, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = result.createGraphics();

		for (int i = 0; i < textures.size(); i++) {

			BufferedImage image = textures.get(i).getBufferedImage(TEXTURE_RESOLUTION);

			int x = (i % numTexturesX) * TEXTURE_RESOLUTION.width;
			int z = (i / numTexturesX) * TEXTURE_RESOLUTION.height;
			g2d.drawImage(image, x, z, null);

		}

		g2d.dispose();

		return result;

	}

	/**
	 * converts a {@link TexCoordFunction} for one of the {@link TextureData}s in this atlas
	 * to a {@link TexCoordFunction} for this atlas
	 */
	public TexCoordFunction mapTexCoords(TextureData texture, TexCoordFunction texCoordFunction) {
		return (List<VectorXYZ> vs) -> texCoordFunction.apply(vs).stream()
				.map(v -> mapTexCoord(texture, v))
				.collect(toList());
	}

	VectorXZ mapTexCoord(TextureData texture, VectorXZ texCoord) {

		int i = textures.indexOf(texture);
		if (i < 0) {
			throw new IllegalArgumentException("Texture is not contained in this atlas: " + texture);
		}

		int slotX = i % numTexturesX;
		int slotZ = i / numTexturesX;

		return new VectorXZ(
				slotX / (double) numTexturesX + texCoord.x / numTexturesX,
				(numTexturesZ - 1 - slotZ) / (double) numTexturesZ + texCoord.z / numTexturesZ); // lower left origin

	}

}