package org.osm2world.scene.material;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static org.osm2world.scene.color.Color.*;

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
import org.osm2world.scene.material.Material.Interpolation;
import org.osm2world.scene.material.Material.Transparency;
import org.osm2world.scene.material.TextTexture.FontStyle;
import org.osm2world.scene.material.TextureData.Wrap;
import org.osm2world.scene.texcoord.NamedTexCoordFunction;
import org.osm2world.scene.texcoord.TexCoordFunction;
import org.osm2world.util.functions.Factory;
import org.osm2world.world.creation.WorldModule;

/**
 * this class defines materials that can be used by all {@link WorldModule}s
 */
public final class Materials {

	/** prevents instantiation */
	private Materials() {}

	/** material for "empty" ground */
	public static final MaterialRef TERRAIN_DEFAULT = new MaterialRef("TERRAIN_DEFAULT",
		new Material(Interpolation.SMOOTH, Color.GREEN));

	public static final MaterialRef WATER = new MaterialRef("WATER",
		new Material(Interpolation.SMOOTH, Color.BLUE));

	public static final MaterialRef ASPHALT = new MaterialRef("ASPHALT",
		new Material(Interpolation.FLAT, new Color(0.3f, 0.3f, 0.3f)));
	public static final MaterialRef BRICK = new MaterialRef("BRICK",
		new Material(Interpolation.FLAT, new Color(1.0f, 0.5f, 0.25f)));
	public static final MaterialRef CONCRETE = new MaterialRef("CONCRETE",
		new Material(Interpolation.FLAT, new Color(0.55f, 0.55f, 0.55f)));
	public static final MaterialRef COPPER_ROOF = new MaterialRef("COPPER_ROOF",
		new Material(Interpolation.FLAT, new Color(195, 219, 185)));
	public static final MaterialRef CORRUGATED_STEEL = new MaterialRef("CORRUGATED_STEEL",
		new Material(Interpolation.FLAT, new Color(200, 200, 200)));
	public static final MaterialRef EARTH = new MaterialRef("EARTH",
		new Material(Interpolation.SMOOTH, new Color(0.3f, 0, 0)));
	public static final MaterialRef GLASS = new MaterialRef("GLASS",
		new Material(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f)));
	public static final MaterialRef GRASS = new MaterialRef("GRASS",
		new Material(Interpolation.SMOOTH, new Color(0.0f, 0.8f, 0.0f)));
	public static final MaterialRef GRASS_PAVER = new MaterialRef("GRASS_PAVER",
		new Material(Interpolation.FLAT, new Color(0.3f, 0.5f, 0.3f)));
	public static final MaterialRef GRAVEL = new MaterialRef("GRAVEL",
		new Material(Interpolation.SMOOTH, new Color(0.4f, 0.4f, 0.4f)));
	public static final MaterialRef ICE = new MaterialRef("ICE",
			new Material(Interpolation.SMOOTH, WHITE));
	public static final MaterialRef SCRUB = new MaterialRef("SCRUB",
		new Material(Interpolation.SMOOTH, new Color(0.0f, 0.8f, 0.0f)));
	public static final MaterialRef SETT = new MaterialRef("SETT",
		new Material(Interpolation.FLAT, new Color(0.3f, 0.3f, 0.3f)));
	public static final MaterialRef SLATE = new MaterialRef("SLATE",
			new Material(Interpolation.FLAT, new Color(0.1f, 0.1f, 0.1f)));
	public static final MaterialRef PAVING_STONE = new MaterialRef("PAVING_STONE",
			new Material(Interpolation.FLAT, new Color(0.4f, 0.4f, 0.4f)));
	public static final MaterialRef PEBBLESTONE = new MaterialRef("PEBBLESTONE",
			new Material(Interpolation.FLAT, new Color(0.4f, 0.4f, 0.4f)));
	public static final MaterialRef PLASTIC = new MaterialRef("PLASTIC",
			new Material(Interpolation.FLAT, new Color(255, 255, 255)));
	public static final MaterialRef ROCK = new MaterialRef("ROCK",
			new Material(Interpolation.FLAT, new Color(160, 166, 155)));
	public static final MaterialRef SAND = new MaterialRef("SAND",
		new Material(Interpolation.SMOOTH, new Color(241, 233, 80)));
	public static final MaterialRef SANDSTONE = new MaterialRef("SANDSTONE",
			new Material(Interpolation.FLAT, new Color(241, 233, 80)));
	public static final MaterialRef SCREE = new MaterialRef("SCREE",
			new Material(Interpolation.FLAT, new Color(160, 166, 155)));
	public static final MaterialRef SNOW = new MaterialRef("SNOW",
			new Material(Interpolation.SMOOTH, WHITE));
	public static final MaterialRef STEEL = new MaterialRef("STEEL",
		new Material(Interpolation.FLAT, new Color(200, 200, 200)));
	public static final MaterialRef STONE = new MaterialRef("STONE",
			new Material(Interpolation.FLAT, new Color(160, 166, 155)));
	public static final MaterialRef UNHEWN_COBBLESTONE = new MaterialRef("UNHEWN_COBBLESTONE",
		new Material(Interpolation.FLAT, new Color(0.3f, 0.3f, 0.3f)));
	public static final MaterialRef WOOD = new MaterialRef("WOOD",
		new Material(Interpolation.FLAT, new Color(0.3f, 0.2f, 0.2f)));
	public static final MaterialRef WOOD_WALL = new MaterialRef("WOOD_WALL",
		new Material(Interpolation.FLAT, new Color(0.3f, 0.2f, 0.2f)));
	public static final MaterialRef TARTAN = new MaterialRef("TARTAN",
		new Material(Interpolation.SMOOTH, new Color(206, 109, 90)));
	public static final MaterialRef TILES = new MaterialRef("TILES",
			new Material(Interpolation.FLAT, WHITE));
	public static final MaterialRef MARBLE = new MaterialRef("MARBLE",
			new Material(Interpolation.SMOOTH, WHITE));
	public static final MaterialRef CARPET = new MaterialRef("CARPET",
			new Material(Interpolation.SMOOTH, new Color(0.5f, 0.5f, 1.0f)));

	public static final MaterialRef ROAD_MARKING = new MaterialRef("ROAD_MARKING",
		new Material(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f)));
	public static final MaterialRef ROAD_MARKING_DASHED = new MaterialRef("ROAD_MARKING_DASHED",
			new Material(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f)));
	public static final MaterialRef ROAD_MARKING_ZEBRA = new MaterialRef("ROAD_MARKING_ZEBRA",
			new Material(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f)));
	public static final MaterialRef ROAD_MARKING_CROSSING = new MaterialRef("ROAD_MARKING_CROSSING",
			new Material(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f)));
	public static final MaterialRef ROAD_MARKING_ARROW_THROUGH = new MaterialRef("ROAD_MARKING_ARROW_THROUGH",
			new Material(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f)));
	public static final MaterialRef ROAD_MARKING_ARROW_THROUGH_RIGHT = new MaterialRef("ROAD_MARKING_ARROW_THROUGH_RIGHT",
			new Material(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f)));
	public static final MaterialRef ROAD_MARKING_ARROW_RIGHT = new MaterialRef("ROAD_MARKING_ARROW_RIGHT",
			new Material(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f)));
	public static final MaterialRef ROAD_MARKING_ARROW_RIGHT_LEFT = new MaterialRef("ROAD_MARKING_ARROW_RIGHT_LEFT",
			new Material(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f)));
	public static final MaterialRef RED_ROAD_MARKING = new MaterialRef("RED_ROAD_MARKING",
			new Material(Interpolation.FLAT, new Color(0.6f, 0.3f, 0.3f)));
	public static final MaterialRef KERB = new MaterialRef("KERB",
			new Material(Interpolation.FLAT, new Color(0.4f, 0.4f, 0.4f)));
	public static final MaterialRef HANDRAIL_DEFAULT = new MaterialRef("HANDRAIL_DEFAULT",
		new Material(Interpolation.FLAT, Color.LIGHT_GRAY));

	public static final MaterialRef RAIL_BALLAST = new MaterialRef("RAIL_BALLAST",
		new Material(Interpolation.SMOOTH, Color.DARK_GRAY));
	public static final MaterialRef RAILWAY = new MaterialRef("RAILWAY",
			new Material(Interpolation.SMOOTH, Color.DARK_GRAY));

	public static final MaterialRef RUNWAY_CENTER_MARKING = new MaterialRef("RUNWAY_CENTER_MARKING",
			new Material(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f)));
	public static final MaterialRef TAXIWAY_CENTER_MARKING = new MaterialRef("TAXIWAY_CENTER_MARKING",
			new Material(Interpolation.FLAT, YELLOW));
	public static final MaterialRef HELIPAD_MARKING = new MaterialRef("HELIPAD_MARKING",
			new Material(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f)));

	public static final MaterialRef BUILDING_DEFAULT = new MaterialRef("BUILDING_DEFAULT",
		new Material(Interpolation.FLAT, new Color(1f, 0.9f, 0.55f)));
	public static final MaterialRef BUILDING_WINDOWS = new MaterialRef("BUILDING_WINDOWS",
		new Material(Interpolation.FLAT, new Color(1f, 0.9f, 0.55f)));
	public static final MaterialRef SINGLE_WINDOW = new MaterialRef("SINGLE_WINDOW",
			new Material(Interpolation.FLAT, WHITE));
	public static final MaterialRef ROOF_DEFAULT = new MaterialRef("ROOF_DEFAULT",
		new Material(Interpolation.FLAT, new Color(0.8f, 0, 0)));
	public static final MaterialRef GLASS_ROOF = new MaterialRef("GLASS_ROOF",
			new Material(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f)));
	public static final MaterialRef GLASS_WALL = new MaterialRef("GLASS_WALL",
			new Material(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f)));
	public static final MaterialRef ENTRANCE_DEFAULT = new MaterialRef("ENTRANCE_DEFAULT",
		new Material(Interpolation.FLAT, new Color(0.2f, 0, 0)));
	public static final MaterialRef GARAGE_DOOR = new MaterialRef("GARAGE_DOOR",
		new Material(Interpolation.FLAT, WHITE));

	public static final MaterialRef GLASS_TRANSPARENT = new MaterialRef("GLASS_TRANSPARENT",
		new Material(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f), Transparency.TRUE, List.of()));

	public static final MaterialRef WALL_GABION = new MaterialRef("WALL_GABION",
		new Material(Interpolation.FLAT, Color.GRAY));

	public static final MaterialRef HEDGE = new MaterialRef("HEDGE",
		new Material(Interpolation.FLAT, new Color(0,0.5f,0)));

	public static final MaterialRef FENCE_DEFAULT = new MaterialRef("FENCE_DEFAULT",
		new Material(Interpolation.FLAT, new Color(0.3f, 0.2f, 0.2f)));
	public static final MaterialRef SPLIT_RAIL_FENCE = new MaterialRef("SPLIT_RAIL_FENCE",
		new Material(Interpolation.FLAT, new Color(0.3f, 0.2f, 0.2f)));
	public static final MaterialRef CHAIN_LINK_FENCE = new MaterialRef("CHAIN_LINK_FENCE",
		new Material(Interpolation.FLAT, new Color(188, 198, 204)));
	public static final MaterialRef METAL_FENCE = new MaterialRef("METAL_FENCE",
		new Material(Interpolation.FLAT, new Color(188, 198, 204)));
	public static final MaterialRef METAL_FENCE_POST = new MaterialRef("METAL_FENCE_POST",
		new Material(Interpolation.FLAT, new Color(188, 198, 204)));

	public static final MaterialRef BRIDGE_DEFAULT = new MaterialRef("BRIDGE_DEFAULT",
		new Material(Interpolation.FLAT, Color.GRAY));
	public static final MaterialRef BRIDGE_PILLAR_DEFAULT = new MaterialRef("BRIDGE_PILLAR_DEFAULT",
		new Material(Interpolation.FLAT, Color.GRAY));

	public static final MaterialRef TUNNEL_DEFAULT = new MaterialRef("TUNNEL_DEFAULT",
		new Material(Interpolation.FLAT, Color.GRAY));

	public static final MaterialRef TREE_TRUNK = new MaterialRef("TREE_TRUNK",
		new Material(Interpolation.SMOOTH, new Color(0.3f, 0.2f, 0.2f)));
	public static final MaterialRef TREE_CROWN = new MaterialRef("TREE_CROWN",
		new Material(Interpolation.SMOOTH, new Color(0, 0.5f, 0)));
	public static final MaterialRef TREE_BILLBOARD_BROAD_LEAVED = new MaterialRef("TREE_BILLBOARD_BROAD_LEAVED",
		new Material(Interpolation.FLAT, new Color(0, 0.5f, 0)));
	public static final MaterialRef TREE_BILLBOARD_BROAD_LEAVED_FRUIT = new MaterialRef("TREE_BILLBOARD_BROAD_LEAVED_FRUIT",
		new Material(Interpolation.FLAT, new Color(0, 0.5f, 0)));
	public static final MaterialRef TREE_BILLBOARD_CONIFEROUS = new MaterialRef("TREE_BILLBOARD_CONIFEROUS",
		new Material(Interpolation.FLAT, new Color(0, 0.5f, 0)));

	public static final MaterialRef POWER_TOWER_VERTICAL = new MaterialRef("POWER_TOWER_VERTICAL",
		new Material(Interpolation.FLAT, new Color(.7f, .7f, .7f),
				Transparency.BINARY, List.of()));
	public static final MaterialRef POWER_TOWER_HORIZONTAL = new MaterialRef("POWER_TOWER_HORIZONTAL",
		new Material(Interpolation.FLAT, new Color(.7f, .7f, .7f),
				Transparency.BINARY, List.of()));

	public static final MaterialRef ADVERTISING_POSTER = new MaterialRef("ADVERTISING_POSTER",
		new Material(Interpolation.FLAT, new Color(1, 1, 0.8f)));

	public static final MaterialRef BUS_STOP_SIGN = new MaterialRef("BUS_STOP_SIGN",
		new Material(Interpolation.FLAT, new Color(0.98f, 0.90f, 0.05f)));

	public static final MaterialRef POSTBOX_DEUTSCHEPOST = new MaterialRef("POSTBOX_DEUTSCHEPOST",
		new Material(Interpolation.FLAT, new Color(1f, 0.8f, 0f)));
	public static final MaterialRef POSTBOX_ROYALMAIL = new MaterialRef("POSTBOX_ROYALMAIL",
		new Material(Interpolation.SMOOTH, new Color(0.8f, 0, 0)));
	public static final MaterialRef TELEKOM_MANGENTA = new MaterialRef("TELEKOM_MANGENTA",
		new Material(Interpolation.FLAT, new Color(0.883f, 0f, 0.453f)));

	public static final MaterialRef FIREHYDRANT = new MaterialRef("FIREHYDRANT",
		new Material(Interpolation.SMOOTH, new Color(0.8f, 0, 0)));

	public static final MaterialRef FLAGCLOTH = new MaterialRef("FLAGCLOTH",
		new Material(Interpolation.SMOOTH, new Color(1f, 1f, 1f)));

	public static final MaterialRef SOLAR_PANEL = new MaterialRef("SOLAR_PANEL",
		new Material(Interpolation.FLAT, Color.BLUE));

	public static final MaterialRef PITCH_BEACHVOLLEYBALL = new MaterialRef("PITCH_BEACHVOLLEYBALL",
		new Material(Interpolation.SMOOTH, new Color(241, 233, 80)));
	public static final MaterialRef PITCH_SOCCER = new MaterialRef("PITCH_SOCCER",
		new Material(Interpolation.SMOOTH, new Color(0.0f, 0.8f, 0.0f)));
	public static final MaterialRef PITCH_TENNIS_ASPHALT = new MaterialRef("PITCH_TENNIS_ASPHALT",
		new Material(Interpolation.SMOOTH, new Color(0.3f, 0.3f, 0.3f)));
	public static final MaterialRef PITCH_TENNIS_CLAY = new MaterialRef("PITCH_TENNIS_CLAY",
		new Material(Interpolation.SMOOTH, new Color(0.8f, 0.0f, 0.0f)));
	public static final MaterialRef PITCH_TENNIS_GRASS = new MaterialRef("PITCH_TENNIS_GRASS",
		new Material(Interpolation.SMOOTH, new Color(0.0f, 0.8f, 0.0f)));
	public static final MaterialRef PITCH_TENNIS_SINGLES_ASPHALT = new MaterialRef("PITCH_TENNIS_SINGLES_ASPHALT",
		new Material(Interpolation.SMOOTH, new Color(0.3f, 0.3f, 0.3f)));
	public static final MaterialRef PITCH_TENNIS_SINGLES_CLAY = new MaterialRef("PITCH_TENNIS_SINGLES_CLAY",
		new Material(Interpolation.SMOOTH, new Color(0.8f, 0.0f, 0.0f)));
	public static final MaterialRef PITCH_TENNIS_SINGLES_GRASS = new MaterialRef("PITCH_TENNIS_SINGLES_GRASS",
		new Material(Interpolation.SMOOTH, new Color(0.0f, 0.8f, 0.0f)));

	public static final MaterialRef TENNIS_NET = new MaterialRef("TENNIS_NET",
		new Material(Interpolation.SMOOTH, Color.WHITE));

	public static final MaterialRef SKYBOX = new MaterialRef("SKYBOX",
		new Material(Interpolation.FLAT, Color.BLUE));

	/** material for "nothingness" which reflects no light. Used e.g. for openings into buildings without indoor. */
	public static final MaterialRef VOID = new MaterialRef("VOID",
			new Material(Interpolation.FLAT, BLACK));

	private static final Map<String, MaterialRef> surfaceMaterialMap;

	private static final Map<String, Material> materialsByName = new HashMap<>();

	static {

		surfaceMaterialMap = new HashMap<>();
		surfaceMaterialMap.put("asphalt", ASPHALT);
		surfaceMaterialMap.put("carpet", CARPET);
		surfaceMaterialMap.put("cobblestone", SETT);
		surfaceMaterialMap.put("compacted", GRAVEL);
		surfaceMaterialMap.put("concrete", CONCRETE);
		surfaceMaterialMap.put("concrete:plates", CONCRETE);
		surfaceMaterialMap.put("dirt", EARTH);
		surfaceMaterialMap.put("earth", EARTH);
		surfaceMaterialMap.put("fine_gravel", GRAVEL);
		surfaceMaterialMap.put("grass", GRASS);
		surfaceMaterialMap.put("gravel", GRAVEL);
		surfaceMaterialMap.put("grass_paver", GRASS_PAVER);
		surfaceMaterialMap.put("ground", EARTH);
		surfaceMaterialMap.put("ice", ICE);
		surfaceMaterialMap.put("marble", MARBLE);
		surfaceMaterialMap.put("mud", EARTH);
		surfaceMaterialMap.put("paved", ASPHALT);
		surfaceMaterialMap.put("paving_stones", PAVING_STONE);
		surfaceMaterialMap.put("pebblestone", PEBBLESTONE);
		surfaceMaterialMap.put("rock", ROCK);
		surfaceMaterialMap.put("salt", SNOW);
		surfaceMaterialMap.put("sand", SAND);
		surfaceMaterialMap.put("scree", SCREE);
		surfaceMaterialMap.put("sett", SETT);
		surfaceMaterialMap.put("snow", SNOW);
		surfaceMaterialMap.put("steel", STEEL);
		surfaceMaterialMap.put("tartan", TARTAN);
		surfaceMaterialMap.put("tiles", TILES);
		surfaceMaterialMap.put("unpaved", EARTH);
		surfaceMaterialMap.put("unhewn_cobblestone", UNHEWN_COBBLESTONE);
		surfaceMaterialMap.put("wood", WOOD);
		surfaceMaterialMap.put("scrub", SCRUB);

	}

	/** returns all materials defined here */
	synchronized public static Collection<Material> getMaterials() {
		return materialsByName.values();
	}

	/**
	 * returns a material defined here based on its name
	 *
	 * @param name  case-insensitive name of the material
	 */
	synchronized public static @Nullable Material getMaterial(@Nullable String name) {
		if (name == null) return null;
		return materialsByName.get(name);
	}

	/** variant of {@link #getMaterial(String)} with a default value */
	synchronized public static Material getMaterial(@Nullable String name, MaterialOrRef defaultValue) {
		Material result = getMaterial(name);
		return result == null ? defaultValue.get() : result;
	}

	public static Material resolveMaterial(MaterialOrRef materialOrRef) {
		if (materialOrRef instanceof MaterialRef materialRef) {
			Material material = getMaterial(materialRef.name());
			return material != null ? material : materialRef.defaultAppearance();
		} else {
			return (Material) materialOrRef;
		}
	}

	/** returns a material for a surface value; null if none is found */
	synchronized public static Material getSurfaceMaterial(String value) {
		return getSurfaceMaterial(value, null);
	}

	/** same as {@link #getSurfaceMaterial(String)}, but with fallback value */
	synchronized public static Material getSurfaceMaterial(String value, MaterialOrRef fallback) {
		MaterialRef materialRef = value == null ? null : surfaceMaterialMap.get(value);
		if (materialRef != null) {
			return materialRef.get();
		} else {
			return fallback == null ? null : fallback.get();
		}
	}

	/**
	 * Returns a human-readable, unique name for a material known within this class,
	 * null for all other materials.
	 * To handle equal materials known under different names,
	 * it attempts to look for object identity before object equality.
	 */
	synchronized public static String getUniqueName(MaterialOrRef material) {

		if (material == null) return null;

		// check by object identity first
		for (var entry : materialsByName.entrySet()) {
			if (entry.getValue() == material.get()) {
				return entry.getKey();
			}
		}

		// check by object equality second
		for (var entry : materialsByName.entrySet()) {
			if (entry.getValue() == material.get()) {
				return entry.getKey();
			}
		}

		// no match found
		return null;

	}

	/**
	 * Returns the transparent variant of a material, if available.
	 * For example, there may be an equivalent of GLASS_WALL that has partially transparent glass panes
	 * and should be used if the space behind the wall is also modeled in 3D.
	 *
	 * @return the transparent variant of the material, or null if none is available
	 */
	public static @Nullable Material getTransparentVariant(MaterialOrRef material) {
		return switch (requireNonNullElse(getUniqueName(material), "")) {
			case "GLASS" -> GLASS_TRANSPARENT.get();
			case "GLASS_WALL" -> getMaterial("GLASS_WALL_TRANSPARENT", null);
			case "GLASS_ROOF" -> getMaterial("GLASS_ROOF_TRANSPARENT", null);
			default -> null;
		};
	}

	private static final Pattern CONF_KEY_PATTERN = Pattern.compile(
					"material_(.+)_(interpolation|color|doubleSided|shadow|ssao|transparency|texture\\d*_.+)");

	/**
	 * configures the attributes of the materials within this class
	 * based on external configuration settings
	 */
	synchronized public static void configureMaterials(O2WConfig config) {

		materialsByName.clear();

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

			Material material = new Material(Interpolation.FLAT, WHITE);

			String keyPrefix = "material_" + materialName + "_";

			for (String attribute : attributes) {

				String key = keyPrefix + attribute;

				switch (attribute) {
					case "doubleSided" -> {
						boolean doubleSided = config.getBoolean(key);
						material = material.withDoubleSided(doubleSided);
					}
					case "interpolation" -> {
						Interpolation interpolation = config.getEnum(Interpolation.class, key);
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
						Transparency transparency = config.getEnum(Transparency.class, key);
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

			materialsByName.put(materialName, material);

		}

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
		Wrap wrap = getWrap(config.getString(keyPrefix + "_wrap"));
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
					int fontStyle = FontStyle.getStyle(values[1].toUpperCase());
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

	private static TextureData createTexture(URI imageURI, TextureDataDimensions dimensions, Wrap wrap, Function<TextureDataDimensions, TexCoordFunction> coordFunction) {
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

	private static Wrap getWrap(String wrapString) {
		if (wrapString != null && wrapString.toLowerCase().startsWith("clamp")) {
			return Wrap.CLAMP;
		} else {
			return Wrap.REPEAT;
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
