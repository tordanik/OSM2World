package org.osm2world.core.target.common.material;

import static java.lang.Math.min;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import org.apache.commons.lang.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.osm2world.core.util.color.LColor;

/**
 * a set of textures for a material.
 *
 * A second texture layer can be thought of as loosely equivalent to a copy of the geometry (with different, partially
 * transparent, texturing) being placed a tiny distance in front of the original geometry.
 *
 * All layers must have at least a base color texture is guaranteed to be present in a particular layer,
 * but at least one texture must be present in each layer.
 */
public class TextureLayer {

	public static enum TextureType {

		BASE_COLOR, NORMAL, ORM, DISPLACEMENT;

		public String fileNameInfix() {
			switch (this) {
			case BASE_COLOR: return "Color";
			case ORM: return "ORM";
			case NORMAL: return "Normal";
			case DISPLACEMENT: return "Displacement";
			default: throw new Error();
			}
		}

	}

	/**
	 * texture for base color and alpha as in glTF 2.0.
	 */
	public final @NonNull TextureData baseColorTexture;

	/**
	 * tangent space normal map as in glTF 2.0.
	 * R [0 to 255] maps to X [-1 to 1]. G [0 to 255] maps to Y [-1 to 1]. B [128 to 255] maps to Z [1/255 to 1]
	 */
	public final @Nullable TextureData normalTexture;

	/**
	 * texture for metalnessRoughness + ambient occlusion as in glTF 2.0.
	 * occlusion (R channel), roughness (G channel), metalness (B channel)
	 */
	public final @Nullable TextureData ormTexture;

	/** displacement map texture */
	public final @Nullable TextureData displacementTexture;

	/** whether the {@link #baseColorTexture} is multiplied with the material color */
	public final boolean colorable;

	public TextureLayer(@NonNull TextureData baseColorTexture, TextureData normalTexture, TextureData ormTexture,
			TextureData displacementTexture, boolean colorable) {
		this.baseColorTexture = baseColorTexture;
		this.normalTexture = normalTexture;
		this.ormTexture = ormTexture;
		this.displacementTexture = displacementTexture;
		this.colorable = colorable;
	}

	/** returns all textures on this layer */
	public List<TextureData> textures() {
		List<TextureData> result = new ArrayList<>(4);
		result.add(baseColorTexture);
		if (normalTexture != null) {
			result.add(normalTexture);
		}
		if (ormTexture != null) {
			result.add(ormTexture);
		}
		if (displacementTexture != null) {
			result.add(displacementTexture);
		}
		return result;
	}

	public @Nullable TextureData getTexture(TextureType type) {
		switch (type) {
		case BASE_COLOR: return baseColorTexture;
		case NORMAL: return normalTexture;
		case ORM: return ormTexture;
		case DISPLACEMENT: return displacementTexture;
		default: throw new Error("Unsupported texture type: " + type);
		}
	}

	/**
	 * calculates the base color factor, a constant factor for the color values of the {@link #baseColorTexture}.
	 * The factor includes a correction for the color of the {@link #baseColorTexture} in order to get the product
	 * as close as possible to the targetBaseColor).
	 *
	 * @param targetBaseColor  the intended average color of the result
	 * @param maxValue  the maximum value for each component of the result, often set to 1.0f
	 *
	 * @return array with 4 components for red, green, blue and alpha
	 */
	public float[] baseColorFactor(LColor targetBaseColor, float maxValue) {

		LColor textureColor = baseColorTexture.getAverageColor();

		return new float[] {
			min(maxValue, targetBaseColor.red / textureColor.red),
			min(maxValue, targetBaseColor.green / textureColor.green),
			min(maxValue, targetBaseColor.blue / textureColor.blue)
		};

	}

	/**
	 * variant of {@link #baseColorFactor(LColor, float)} that clamps each component to the range [0,1]
	 * and returns {@link LColor}
	 */
	public LColor clampedBaseColorFactor(LColor targetBaseColor) {
		return new LColor(baseColorFactor(targetBaseColor, 1.0f));
	}

	/**
	 * writes this layer's textures to files
	 *
	 * @param baseFileName  the file name to use, with an "$INFIX" placeholder to be replaced with "Color", "ORM" etc.
	 */
	public void writeToFiles(File baseFileName) throws IOException {

		if (!baseFileName.getAbsolutePath().contains("$INFIX")) {
			throw new IllegalArgumentException("File path must contain an '$INFIX' placeholder");
		}

		for (TextureType type : TextureType.values()) {

			TextureData texture = this.getTexture(type);

			if (texture != null) {
				File file = new File(baseFileName.getAbsolutePath().replace("$INFIX", type.fileNameInfix()));
				ImageIO.write(texture.getBufferedImage(), "png", file);
			}

		}

	}

	@Override
	public String toString() {

		String[] textureNames = textures().stream().map(it -> it.toString()).toArray(String[]::new);

		String commonPrefix = StringUtils.getCommonPrefix(textureNames);
		int index = commonPrefix.lastIndexOf("_");

		if (textureNames.length == 1) {
			return textureNames[0].replaceAll("\\.(png|jpg|svg)", "");
		} else if (index > 0) {
			return commonPrefix.subSequence(0, index).toString();
		} else {
			return "TextureLayer [baseColorTexture=" + baseColorTexture
					+ ", normalTexture=" + normalTexture
					+ ", ormTexture=" + ormTexture
					+ ", displacementTexture=" + displacementTexture
					+ ", colorable=" + colorable + "]";
		}

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ormTexture == null) ? 0 : ormTexture.hashCode());
		result = prime * result + ((baseColorTexture == null) ? 0 : baseColorTexture.hashCode());
		result = prime * result + ((normalTexture == null) ? 0 : normalTexture.hashCode());
		result = prime * result + ((displacementTexture == null) ? 0 : displacementTexture.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof TextureLayer
				&& Objects.equals(((TextureLayer)obj).baseColorTexture, baseColorTexture)
				&& Objects.equals(((TextureLayer)obj).normalTexture, normalTexture)
				&& Objects.equals(((TextureLayer)obj).ormTexture, ormTexture)
				&& Objects.equals(((TextureLayer)obj).displacementTexture, displacementTexture)
				&& ((TextureLayer)obj).colorable == colorable;
	}

}