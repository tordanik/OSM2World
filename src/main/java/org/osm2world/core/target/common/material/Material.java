package org.osm2world.core.target.common.material;

import static java.util.Collections.emptyList;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osm2world.core.map_data.data.TagSet;

/**
 * describes the material/surface properties of an object for lighting
 */
public abstract class Material {

	/** maximum number of {@link TextureLayer}s any material can use */
	public static final int MAX_TEXTURE_LAYERS = 32;

	public static enum Interpolation {FLAT, SMOOTH}

	public static enum Transparency {
		/** arbitrary transparency, including partially transparent pixels */
		TRUE,
		/** only allow pixels to be either fully transparent or fully opaque */
		BINARY,
		/** all pixels are opaque */
		FALSE
	}

	public static enum Shadow {
		/** casts shadows */
		TRUE,
		/** casts no shadows */
		FALSE
	}

	public static enum AmbientOcclusion {
		/** casts AO */
		TRUE,
		/** casts no AO */
		FALSE
	}

	/**
	 * Interpolation of normals
	 */
	protected Interpolation interpolation;
	protected Color color;
	protected boolean doubleSided;
	protected Transparency transparency;
	protected Shadow shadow;
	protected AmbientOcclusion ambientOcclusion;

	protected List<TextureLayer> textureLayers;

	public Material(Interpolation interpolation, Color color, boolean doubleSided,
			Transparency transparency, Shadow shadow, AmbientOcclusion ambientOcclusion,
			List<TextureLayer> textureLayers) {

		if (textureLayers != null && textureLayers.size() > MAX_TEXTURE_LAYERS) {
			throw new IllegalArgumentException("too many texture layers: " + textureLayers.size());
		}

		this.interpolation = interpolation;
		this.color = color;
		this.doubleSided = doubleSided;
		this.transparency = transparency;
		this.shadow = shadow;
		this.ambientOcclusion = ambientOcclusion;
		this.textureLayers = textureLayers;

	}

	public Material(Interpolation interpolation, Color color,
			Transparency transparency, List<TextureLayer> textureLayers) {
		this(interpolation, color, false, transparency, Shadow.TRUE, AmbientOcclusion.TRUE, textureLayers);
	}

	public Material(Interpolation interpolation, Color color) {
		this(interpolation, color, Transparency.FALSE, emptyList());
	}

	public Interpolation getInterpolation() {
		return interpolation;
	}

	public Color getColor() {
		return color;
	}

	public boolean isDoubleSided() {
		return doubleSided;
	}

	public static final Color multiplyColor(Color c, float factor) {
		float[] colorComponents = new float[3];
		c.getColorComponents(colorComponents);

		return new Color(
				colorComponents[0] * factor,
				colorComponents[1] * factor,
				colorComponents[2] * factor);
	}

	public Material makeSmooth() {
		return new ImmutableMaterial(Interpolation.SMOOTH, getColor(), isDoubleSided(),
				getTransparency(), getShadow(), getAmbientOcclusion(), getTextureLayers());
	}

	/**
	 * returns a material that is like this one,
	 * except with a different list of {@link TextureLayer}s
	 */
	public Material withLayers(List<TextureLayer> textureLayers) {
		if (textureLayers.equals(this.textureLayers)) {
			return this;
		} else {
		    return new ImmutableMaterial(getInterpolation(), getColor(), isDoubleSided(),
		    		getTransparency(), getShadow(), getAmbientOcclusion(), textureLayers);
		}
	}

	/**
	 * returns a material that is like this one,
	 * except with additional {@link TextureLayer}s stacked on top
	 */
	public Material withAddedLayers(List<TextureLayer> additionalTextureLayers) {

		if (additionalTextureLayers.isEmpty()) return this;

		List<TextureLayer> newTextureLayerList = new ArrayList<>(getTextureLayers());
		newTextureLayerList.addAll(additionalTextureLayers);
	    return this.withLayers(newTextureLayerList);

	}

	/**
	 * returns a material that is the same as this one, except with a different color.
	 * @param color  the color to use. Can be null, in which case this material is returned unaltered.
	 */
	public Material withColor(Color color) {

		if (color == null) return this;

		return new ImmutableMaterial(getInterpolation(), color, isDoubleSided(),
				getTransparency(), getShadow(), getAmbientOcclusion(), getTextureLayers());

	}

	/**
	 * returns a copy of {@code material} with its {@link TextTexture} layer
	 * No. {@code numberOfTextLayer} changed with a replica with textColor={@code color}
	 */
	public Material withTextColor(Color color, int numberOfTextLayer) {

		if (getTextureLayers().isEmpty()) return this;

		//copy of the material textureDataList
		List<TextureLayer> textureDataList = new ArrayList<>(getTextureLayers());

		int counter = 0;

		for (TextureLayer layer : getTextureLayers()) {

			if (layer.baseColorTexture instanceof TextTexture) {

				TextTexture texture = (TextTexture)layer.baseColorTexture;

				if(counter==numberOfTextLayer) {

					//create a new TextTextureData instance with different textColor
					TextTexture newTextTexture = new TextTexture(texture.text, texture.font, texture.width,
							texture.height, texture.widthPerEntity, texture.heightPerEntity,
							texture.topOffset, texture.leftOffset,
							color, texture.relativeFontSize,
							texture.wrap, t -> texture.coordFunction);

					textureDataList.set(numberOfTextLayer, new TextureLayer(newTextTexture,
							layer.normalTexture, layer.ormTexture, layer.displacementTexture, layer.colorable));

					//return a copy of the material with the new textureDataList
					return new ImmutableMaterial(getInterpolation(),getColor(), isDoubleSided(),
							getTransparency(), getShadow(), getAmbientOcclusion(), textureDataList);

				}

				counter++;
			}
		}

		return this;
	}

	/**
	 * Fills in placeholders, such as <code>%{name}</code>, in this material's {@link TextTexture}s.
	 * To fill in a placeholder, it looks for the placeholder variable in the map parameter, then in the tags.
	 * Fallback values in placeholders are supported as well, using <code>%{name, fallback}</code> syntax.
	 *
	 * @param map  map from placeholder variables to the text they should be replaced with
	 * @param tags  the {@link TagSet} to extract values from
	 * @return  a replica of this material with a modified list of {@link TextureLayer}s,
	 * or this material itself if it has no {@link TextTexture}s with placeholders.
	 */
	public Material withPlaceholdersFilledIn(Map<String, String> map, TagSet tags) {

		List<TextureLayer> newLayers = new ArrayList<>();

		for (TextureLayer layer : this.getTextureLayers()) {

			newLayers.add(new TextureLayer(
					withPlaceholdersFilledIn(layer.baseColorTexture, map, tags),
					withPlaceholdersFilledIn(layer.normalTexture, map, tags),
					withPlaceholdersFilledIn(layer.ormTexture, map, tags),
					withPlaceholdersFilledIn(layer.displacementTexture, map, tags),
					layer.colorable));

		}

		return this.withLayers(newLayers);

	}

	private static TextureData withPlaceholdersFilledIn(TextureData texture, Map<String, String> map, TagSet tags) {

		if (texture instanceof TextTexture) {

			TextTexture textTexture = (TextTexture) texture;

			Pattern pattern = Pattern.compile("%\\{([^,\\}]+)(?:,\\s*([^\\}]+))?\\}");

			String newText = textTexture.text;
			Matcher matcher = pattern.matcher(newText);

			while (matcher.find()) {

				String key = matcher.group(1);
				String replacement;

				if (map.containsKey(key)) {
					replacement = map.get(key);
				} else if (tags.containsKey(key)) {
					replacement = tags.getValue(key);
				} else if (matcher.group(2) != null) {
					replacement = matcher.group(2);
				} else {
					System.err.println("Unknown placeholder key: " + key);
					replacement = "";
				}

				newText = newText.replace(matcher.group(0), replacement);

			}

			if (!newText.equals(textTexture.text)) {
				return new TextTexture(newText,
						textTexture.font, textTexture.width, textTexture.height,
						textTexture.widthPerEntity, textTexture.heightPerEntity,
						textTexture.topOffset, textTexture.leftOffset,
						textTexture.textColor, textTexture.relativeFontSize,
						textTexture.wrap, t -> textTexture.coordFunction);
			}

		}

		return texture;

	}

	public Transparency getTransparency() {
		return transparency;
	}

	public Shadow getShadow() {
		return shadow;
	}

	public AmbientOcclusion getAmbientOcclusion() {
		return ambientOcclusion;
	}

	public List<TextureLayer> getTextureLayers() {
		return textureLayers;
	}

	public int getNumTextureLayers() {
		if (textureLayers == null) {
			return 0;
		} else {
			return textureLayers.size();
		}
	}

	@Override
	public String toString() {
		String colorString = String.format(Locale.ROOT, "#%06x", color.getRGB() & 0x00ffffff);
		if (textureLayers.isEmpty() || textureLayers.stream().anyMatch(it -> it.colorable)) {
			return colorString + ", " + textureLayers;
		} else {
			return textureLayers.toString();
		}
	}

}
