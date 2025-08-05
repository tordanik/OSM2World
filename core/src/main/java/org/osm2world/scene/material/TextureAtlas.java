package org.osm2world.scene.material;

import static java.lang.Math.ceil;
import static java.lang.Math.sqrt;
import static java.util.stream.Collectors.toList;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.osm2world.math.VectorXZ;
import org.osm2world.scene.texcoord.TexCoordFunction;
import org.osm2world.util.Resolution;

import com.google.common.base.Objects;

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

	public final List<TextureData> textures;

	private final int numTexturesX;
	private final int numTexturesZ;

	public TextureAtlas(List<TextureData> textures) {

		super(new TextureDataDimensions(1, 1), Wrap.CLAMP, null);

		if (textures.isEmpty()) {
			throw new IllegalArgumentException("empty texture atlas");
		}

		this.textures = textures;

		this.numTexturesX = (int) ceil(sqrt(textures.size()));
		this.numTexturesZ = (int) ceil(textures.size() / (double)numTexturesX);

	}

	@Override
	protected BufferedImage createBufferedImage() {

		BufferedImage result = new BufferedImage(getResolution().width, getResolution().height, BufferedImage.TYPE_INT_ARGB);

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
	public List<VectorXZ> mapTexCoords(TextureData texture, List<VectorXZ> texCoords) {
		return texCoords.stream()
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

	private Resolution getResolution() {
		return new Resolution(numTexturesX * TEXTURE_RESOLUTION.width,
				numTexturesZ * TEXTURE_RESOLUTION.height);
	}

	@Override
	public float getAspectRatio() {
		return getResolution().getAspectRatio();
	}

	@Override
	public String toString() {
		return "TextureAtlas " + textures;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof TextureAtlas)) return false;
		TextureAtlas other = (TextureAtlas) obj;
		return dimensions().equals(other.dimensions())
				&& Objects.equal(wrap, other.wrap)
				&& Objects.equal(coordFunction, other.coordFunction)
				&& Objects.equal(textures, other.textures)
				&& numTexturesX == other.numTexturesX
				&& numTexturesZ == other.numTexturesZ;
	}

	@Override
	public int hashCode() {
		HashCodeBuilder builder = new HashCodeBuilder();
		builder.append(dimensions());
		builder.append(wrap);
		builder.append(coordFunction);
		builder.append(textures);
		builder.append(numTexturesX);
		builder.append(numTexturesZ);
		return builder.toHashCode();
	}

}