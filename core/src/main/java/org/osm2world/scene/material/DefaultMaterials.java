package org.osm2world.scene.material;

import static org.osm2world.scene.color.Color.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.Material.Interpolation;
import org.osm2world.scene.material.Material.Transparency;

/**
 * This class defines materials which are always available
 * (regardless of the specific {@link org.osm2world.style.Style})
 * and can therefore be freely used by all {@link org.osm2world.world.data.WorldObject}s.
 */
public final class DefaultMaterials {

	/** prevents instantiation */
	private DefaultMaterials() {}

	/** material for "empty" ground */
	public static final MaterialRef TERRAIN_DEFAULT = new MaterialRef("TERRAIN_DEFAULT",
		new Material(Interpolation.SMOOTH, Color.GREEN));

	public static final MaterialRef WATER = new MaterialRef("WATER",
		new Material(Interpolation.SMOOTH, Color.BLUE));

	public static final MaterialRef ADOBE = new MaterialRef("ADOBE",
		new Material(Interpolation.FLAT, Color.decode("#c7a17a")));
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
	public static final MaterialRef SHELLS = new MaterialRef("SHELLS",
			new Material(Interpolation.SMOOTH, Color.decode("#9ea5ad")));
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
	public static final MaterialRef THATCH_ROOF = new MaterialRef("THATCH_ROOF",
			new Material(Interpolation.FLAT, Color.decode("#7c7062")));
	public static final MaterialRef UNHEWN_COBBLESTONE = new MaterialRef("UNHEWN_COBBLESTONE",
		new Material(Interpolation.FLAT, new Color(0.3f, 0.3f, 0.3f)));
	public static final MaterialRef WOOD = new MaterialRef("WOOD",
		new Material(Interpolation.FLAT, new Color(0.3f, 0.2f, 0.2f)));
	public static final MaterialRef WOODCHIPS = new MaterialRef("WOODCHIPS",
		new Material(Interpolation.SMOOTH, Color.decode("#b09166")));
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
	private static final List<MaterialRef> defaultMaterials;

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
		surfaceMaterialMap.put("shells", SHELLS);
		surfaceMaterialMap.put("snow", SNOW);
		surfaceMaterialMap.put("steel", STEEL);
		surfaceMaterialMap.put("tartan", TARTAN);
		surfaceMaterialMap.put("tiles", TILES);
		surfaceMaterialMap.put("unpaved", EARTH);
		surfaceMaterialMap.put("unhewn_cobblestone", UNHEWN_COBBLESTONE);
		surfaceMaterialMap.put("wood", WOOD);
		surfaceMaterialMap.put("woodchips", WOODCHIPS);
		surfaceMaterialMap.put("scrub", SCRUB);

		defaultMaterials = List.of(
				TERRAIN_DEFAULT,
				WATER,
				ADOBE,
				ASPHALT,
				BRICK,
				CONCRETE,
				COPPER_ROOF,
				CORRUGATED_STEEL,
				EARTH,
				GLASS,
				GRASS,
				GRASS_PAVER,
				GRAVEL,
				ICE,
				SCRUB,
				SETT,
				SHELLS,
				SLATE,
				PAVING_STONE,
				PEBBLESTONE,
				PLASTIC,
				ROCK,
				SAND,
				SANDSTONE,
				SCREE,
				SNOW,
				STEEL,
				STONE,
				THATCH_ROOF,
				UNHEWN_COBBLESTONE,
				WOOD,
				WOODCHIPS,
				WOOD_WALL,
				TARTAN,
				TILES,
				MARBLE,
				CARPET,ROAD_MARKING,
				ROAD_MARKING_DASHED,
				ROAD_MARKING_ZEBRA,
				ROAD_MARKING_CROSSING,
				ROAD_MARKING_ARROW_THROUGH,
				ROAD_MARKING_ARROW_THROUGH_RIGHT,
				ROAD_MARKING_ARROW_RIGHT,
				ROAD_MARKING_ARROW_RIGHT_LEFT,
				RED_ROAD_MARKING,
				KERB,
				HANDRAIL_DEFAULT,RAIL_BALLAST,
				RAILWAY,RUNWAY_CENTER_MARKING,
				TAXIWAY_CENTER_MARKING,
				HELIPAD_MARKING,
				BUILDING_DEFAULT,
				BUILDING_WINDOWS,
				SINGLE_WINDOW,
				ROOF_DEFAULT,
				GLASS_ROOF,
				GLASS_WALL,
				ENTRANCE_DEFAULT,
				GARAGE_DOOR,
				GLASS_TRANSPARENT,
				WALL_GABION,
				HEDGE,
				FENCE_DEFAULT,
				SPLIT_RAIL_FENCE,
				CHAIN_LINK_FENCE,
				METAL_FENCE,
				METAL_FENCE_POST,
				BRIDGE_DEFAULT,
				BRIDGE_PILLAR_DEFAULT,
				TUNNEL_DEFAULT,
				TREE_TRUNK,
				TREE_CROWN,
				TREE_BILLBOARD_BROAD_LEAVED,
				TREE_BILLBOARD_BROAD_LEAVED_FRUIT,
				TREE_BILLBOARD_CONIFEROUS,
				POWER_TOWER_VERTICAL,
				POWER_TOWER_HORIZONTAL,
				ADVERTISING_POSTER,
				BUS_STOP_SIGN,
				POSTBOX_DEUTSCHEPOST,
				POSTBOX_ROYALMAIL,
				TELEKOM_MANGENTA,
				FIREHYDRANT,
				FLAGCLOTH,
				SOLAR_PANEL,
				PITCH_BEACHVOLLEYBALL,
				PITCH_SOCCER,
				PITCH_TENNIS_ASPHALT,
				PITCH_TENNIS_CLAY,
				PITCH_TENNIS_GRASS,
				PITCH_TENNIS_SINGLES_ASPHALT,
				PITCH_TENNIS_SINGLES_CLAY,
				PITCH_TENNIS_SINGLES_GRASS,
				TENNIS_NET,
				SKYBOX,
				VOID
		);

	}

	/** returns all default materials defined here */
	public static List<MaterialRef> getDefaultMaterials() {
		return defaultMaterials;
	}

	/** returns a material reference for a surface value; null if none is found */
	public static MaterialOrRef getSurfaceMaterialRef(String value) {
		return getSurfaceMaterialRef(value, null);
	}

	/** returns a material for a surface value; null if none is found */
	public static Material getSurfaceMaterial(String value, O2WConfig config) {
		return config.mapStyle().resolveMaterial(getSurfaceMaterialRef(value));
	}

	/** same as {@link #getSurfaceMaterialRef(String)}, but with fallback value */
	public static MaterialOrRef getSurfaceMaterialRef(String value, MaterialOrRef fallback) {
		MaterialRef materialRef = value == null ? null : surfaceMaterialMap.get(value);
		return materialRef != null ? materialRef : fallback;
	}

	/** same as {@link #getSurfaceMaterial(String, O2WConfig)}, but with fallback value */
	public static Material getSurfaceMaterial(String value, MaterialOrRef fallback, O2WConfig config) {
		return config.mapStyle().resolveMaterial(getSurfaceMaterialRef(value, fallback));
	}

}
