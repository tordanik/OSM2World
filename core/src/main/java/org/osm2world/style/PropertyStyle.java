package org.osm2world.style;

import static java.util.Objects.requireNonNull;
import static org.osm2world.scene.color.Color.WHITE;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.*;
import org.osm2world.scene.texcoord.NamedTexCoordFunction;
import org.osm2world.scene.texcoord.TexCoordFunction;
import org.osm2world.util.functions.Factory;

/**
 * A map style based on properties files.
 */
public class PropertyStyle implements Style {

	private final Map<String, Material> materialsByName;

	private static final Pattern CONF_KEY_PATTERN = Pattern.compile(
			"material_(.+)_(interpolation|color|doubleSided|shadow|ssao|transparency|texture\\d*_.+)");

	/**
	 * @param config  a configuration object which provides access to the properties
	 */
	public PropertyStyle(O2WConfig config) {

		materialsByName = new HashMap<>();

		/* find all material-related properties and organize them by material */

		Map<String, Set<String>> attributesPerMaterialName = new HashMap<>();

		Iterator<String> keyIterator = config.getKeys();

		while (keyIterator.hasNext()) {
			String key = keyIterator.next();
			Matcher matcher = CONF_KEY_PATTERN.matcher(key);
			if (matcher.matches()) {
				String materialName = matcher.group(1);
				if (!attributesPerMaterialName.containsKey(materialName)) {
					attributesPerMaterialName.put(materialName, new HashSet<>());
				}
				attributesPerMaterialName.get(materialName).add(matcher.group(2));
			}
		}

		/* create each material */

		for (var entry : attributesPerMaterialName.entrySet()) {

			String materialName = entry.getKey();
			Set<String> attributes = entry.getValue();

			Material material = new Material(Material.Interpolation.FLAT, WHITE);

			String keyPrefix = "material_" + materialName + "_";

			for (String attribute : attributes) {

				String key = keyPrefix + attribute;

				switch (attribute) {
					case "doubleSided" -> {
						boolean doubleSided = config.getBoolean(key);
						material = material.withDoubleSided(doubleSided);
					}
					case "interpolation" -> {
						Material.Interpolation interpolation = config.getEnum(Material.Interpolation.class, key);
						if (interpolation != null) { material = material.withInterpolation(interpolation); }
					}
					case "shadow" -> {
						Material.Shadow shadow = config.getEnum(Material.Shadow.class, key);
						if (shadow != null) { material = material.withShadow(shadow); }
					}
					case "ssao" -> {
						Material.AmbientOcclusion ao = config.getEnum(Material.AmbientOcclusion.class, key);
						if (ao != null) { material = material.withAmbientOcclusion(ao); }
					}
					case "transparency" -> {
						Material.Transparency transparency = config.getEnum(Material.Transparency.class, key);
						if (transparency != null) { material = material.withTransparency(transparency); }
					}
					case "color" -> {
						Color color = config.getColor(key);
						if (color != null) {
							material = material.withColor(color);
						} else {
							System.err.println("incorrect color value: " + config.getString(key));
						}
					}
					default -> {
						if (!attribute.startsWith("texture")) {
							System.err.println("unknown material attribute '" + attribute + "' for material " + materialName);
						}
					}
				}

			}

			/* configure texture layers */

			List<TextureLayer> textureLayers = new ArrayList<>();

			for (int i = 0; i < Material.MAX_TEXTURE_LAYERS; i++) {
				String attribute = "texture" + i;
				if (attributes.stream().anyMatch(a -> a.startsWith(attribute))) {
					boolean implicitColorTexture = attributes.stream().noneMatch(a -> a.startsWith(attribute + "_color_"));
					TextureLayer textureLayer = createTextureLayer(config, keyPrefix + attribute, implicitColorTexture);
					if (textureLayer != null) {
						textureLayers.add(textureLayer);
					}
				} else {
					break;
				}
			}

			material = material.withLayers(textureLayers);

			/* store the finished material */

			materialsByName.put(materialName.toUpperCase(Locale.ROOT), material);

		}

	}

	@Override
	public Collection<Material> getMaterials() {
		return materialsByName.values();
	}

	@Override
	public @Nullable Material resolveMaterial(@Nullable String name) {
		if (name == null) return null;
		return materialsByName.get(name.toUpperCase(Locale.ROOT));
	}

	@Override
	public String getUniqueName(MaterialOrRef material) {

		if (material == null) return null;

		Material m = resolveMaterial(material);

		// check by object identity first
		for (var entry : materialsByName.entrySet()) {
			if (entry.getValue().equals(m)) {
				return entry.getKey();
			}
		}

		// check by object equality second
		for (var entry : materialsByName.entrySet()) {
			if (entry.getValue().equals(m)) {
				return entry.getKey();
			}
		}

		// no match found
		return null;

	}

	private static @Nullable TextureLayer createTextureLayer(O2WConfig config, String keyPrefix, boolean implicitColorTexture) {

		URI baseColorTexture = null;
		URI ormTexture = null;
		URI normalTexture = null;
		URI displacementTexture = null;

		if (config.containsKey(keyPrefix + "_dir")) {

			URI textureDirURI = config.resolveFileConfigProperty(config.getString(keyPrefix + "_dir"));

			if (textureDirURI != null && "file".equals(textureDirURI.getScheme())
					&& new File(textureDirURI).exists() && new File(textureDirURI).isDirectory()) {

				for (File file : requireNonNull(new File(textureDirURI).listFiles())) {
					if (file.getName().contains("_Color.")) {
						baseColorTexture = file.toURI();
					} else if (file.getName().contains("_ORM.")) {
						ormTexture = file.toURI();
					} else if (file.getName().contains("_Normal.")) {
						normalTexture = file.toURI();
					} else if (file.getName().contains("_Displacement.")) {
						displacementTexture = file.toURI();
					}
				}

			} else if (textureDirURI != null && List.of("http", "https").contains(textureDirURI.getScheme())) {

				URI parentURI = textureDirURI.toString().endsWith("/")
						? textureDirURI
						: URI.create(textureDirURI + "/");

				String[] pathParts = textureDirURI.getPath().split("/");

				if (pathParts.length >= 2) {

					String materialName = pathParts[pathParts.length - 1];

					BiFunction<URI, Factory<URI>, URI> uriIfExistsElse = (URI uri, Factory<URI> fallback) -> {
						try {
							HttpURLConnection huc = (HttpURLConnection) uri.toURL().openConnection();
							huc.setRequestMethod("HEAD");
							if (huc.getResponseCode() == HttpURLConnection.HTTP_OK) {
								return uri;
							}
						} catch (IOException ignored) {}
						return fallback.get();
					};

					baseColorTexture =
							uriIfExistsElse.apply(parentURI.resolve(materialName + "_Color.png"),
									() -> uriIfExistsElse.apply(parentURI.resolve(materialName + "_Color.jpg"),
											() -> uriIfExistsElse.apply(parentURI.resolve(materialName + "_Color.jpeg"),
													() -> null)));

					// TODO other types

				}

			} else {
				System.err.println("Not a directory: " + textureDirURI);
			}
		}

		TextureData baseColorTextureData = createTexture(
				config, keyPrefix + (implicitColorTexture ? "" : "_color"), baseColorTexture);

		if (baseColorTextureData == null) {
			System.err.println("Config is missing base color texture for " + keyPrefix);
			return null;
		} else {
			return new TextureLayer(
					baseColorTextureData,
					createTexture(config, keyPrefix + "_normal", normalTexture),
					createTexture(config, keyPrefix + "_orm", ormTexture),
					createTexture(config, keyPrefix + "_displacement", displacementTexture),
					config.getBoolean(keyPrefix + "_colorable", false));
		}

	}

	/**
	 * @param defaultImageURI  texture file to use if there's no _file attribute
	 * @return  valid {@link TextureData} extracted from the config file, or null
	 */
	private static @Nullable TextureData createTexture(O2WConfig config, String keyPrefix,
			@Nullable URI defaultImageURI) {

		TextureDataDimensions dimensions = createTextureDataDimensions(config, keyPrefix);
		TextureData.Wrap wrap = getWrap(config.getString(keyPrefix + "_wrap"));
		@Nullable Function<TextureDataDimensions, TexCoordFunction> coordFunction =
				getCoordFunction(config.getString(keyPrefix + "_coord_function"));

		//get texture layer type
		String type = config.getString(keyPrefix + "_type", "image");

		if ("text".equals(type)) {

			String fontKey = keyPrefix + "_font";
			String textKey = keyPrefix + "_text";
			String topOffsetKey = keyPrefix + "_topOffset";
			String leftOffsetKey = keyPrefix + "_leftOffset";
			String relativeFontSizeKey = keyPrefix + "_relative_font_size";
			String textColorKey = keyPrefix + "_textColor";

			String text = "";

			//get text configuration
			if (config.getString(textKey) != null) {
				text = config.getString(textKey);
			}

			//get font configuration
			Font font = null;
			if (config.getString(fontKey) == null) {

				font = new Font("Dialog", Font.PLAIN, 100);

			} else {

				String[] values = config.getString(fontKey).split(",", 2);

				if (values.length == 2) {
					int fontStyle = TextTexture.FontStyle.getStyle(values[1].toUpperCase());
					font = new Font(values[0], fontStyle, 100);
				} else {
					font = new Font("Dialog", Font.PLAIN, 100);
				}
			}

			//get top/left offset configuration
			String topOffset = config.getString(topOffsetKey);
			if (topOffset != null) {
				if (topOffset.endsWith("%")) {
					topOffset = topOffset.substring(0, topOffset.length() - 1);
				}
			} else {
				topOffset = Integer.toString(50);
			}

			String leftOffset = config.getString(leftOffsetKey);
			if (leftOffset != null) {
				if (leftOffset.endsWith("%")) {
					leftOffset = leftOffset.substring(0, leftOffset.length() - 1);
				}
			} else {
				leftOffset = Integer.toString(50);
			}

			//get text color configuration
			Color color = config.getColor(textColorKey, Color.BLACK);

			//get relative font size
			double relativeFontSize = config.getDouble(relativeFontSizeKey, 60);

			return new TextTexture(text, font, dimensions,
					Double.parseDouble(topOffset), Double.parseDouble(leftOffset), color,
					relativeFontSize, wrap, coordFunction);

		} else if ("image".equals(type)) {

			URI imageURI = config.resolveFileConfigProperty(config.getString(keyPrefix + "_file"));

			if (imageURI != null) {
				return createTexture(imageURI, dimensions, wrap, coordFunction);
			} else if (defaultImageURI != null) {
				return createTexture(defaultImageURI, dimensions, wrap, coordFunction);
			} else {
				return null;
			}

		} else {
			System.err.println("unknown type value: " + type);
			return null;
		}

	}

	private static TextureData createTexture(URI imageURI, TextureDataDimensions dimensions, TextureData.Wrap wrap, Function<TextureDataDimensions, TexCoordFunction> coordFunction) {
		if ("file".equals(imageURI.getScheme())) {
			return ImageFileTexture.create(new File(imageURI), dimensions, wrap, coordFunction);
		} else {
			return new UriTexture(imageURI, dimensions, wrap, coordFunction);
		}
	}

	/**
	 * @return  valid {@link TextureDataDimensions} extracted from the config file, possibly using default values
	 */
	private static TextureDataDimensions createTextureDataDimensions(O2WConfig config, String keyPrefix) {

		keyPrefix  = keyPrefix.replaceFirst("_(?:color|normal|orm|displacement)$", "");

		double width = config.getDouble(keyPrefix + "_width", 1.0);
		double height = config.getDouble(keyPrefix + "_height", 1.0);

		Double widthPerEntity = config.getDouble(keyPrefix + "_widthPerEntity", null);
		Double heightPerEntity = config.getDouble(keyPrefix + "_heightPerEntity", null);

		double padding = config.getDouble(keyPrefix + "_padding", 0);

		if (width <= 0) {
			System.err.println("Error: illegal width for texture " + keyPrefix);
			width = 1;
		}

		if (height <= 0) {
			System.err.println("Error: illegal height for texture " + keyPrefix);
			height = 1;
		}

		return new TextureDataDimensions(width, height, widthPerEntity, heightPerEntity, padding);

	}

	private static TextureData.Wrap getWrap(String wrapString) {
		if (wrapString != null && wrapString.toLowerCase().startsWith("clamp")) {
			return TextureData.Wrap.CLAMP;
		} else {
			return TextureData.Wrap.REPEAT;
		}
	}

	private static @Nullable Function<TextureDataDimensions, TexCoordFunction> getCoordFunction(
			String coordFunctionString) {

		Function<TextureDataDimensions, TexCoordFunction> result = null;

		if (coordFunctionString != null) {
			result = NamedTexCoordFunction.valueOf(coordFunctionString.toUpperCase());
		}

		return result;
	}

}
