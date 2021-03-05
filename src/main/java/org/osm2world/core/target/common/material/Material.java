package org.osm2world.core.target.common.material;

import static java.util.Collections.emptyList;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

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
	 * returns a material that is the same as this one,
	 * except with additional texture data layers stacked on top
	 */
	public Material withAddedLayers(List<TextureLayer> textureLayers) {

		if (textureLayers.isEmpty()) return this;

		List<TextureLayer> textureDataList = new ArrayList<>(getTextureLayers());

		textureDataList.addAll(textureLayers);

	    return new ImmutableMaterial(getInterpolation(), getColor(), isDoubleSided(),
	    		getTransparency(), getShadow(), getAmbientOcclusion(), textureDataList);

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
							texture.wrap, texture.coordFunction);

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
		return String.format("{%s, #%06x, a%3f, d%3f, s%3f, sh%d, %d tex, ",
				interpolation, color.getRGB() & 0x00ffffff, textureLayers.size())
				+ transparency + shadow + ambientOcclusion
				+ "}";
	}

	/*
	 * some possible later additions: specular (obvious ...),
	 * as well as brilliance, phong, metallic, reflection, crand and iridescence for POVRay
	 */

}
