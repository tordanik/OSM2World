package org.osm2world.world.modules.building;

import org.osm2world.map_data.data.TagSet;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.Materials;

/** default properties for a particular building or building:part type. Immutable. */
public class BuildingDefaults {

	public final int levels;
	public final double heightPerLevel;
	public final String roofShape;
	public final Material materialWall;
	public final Material materialRoof;
	public final boolean hasWindows;
	public final boolean hasWalls;

	public BuildingDefaults(int levels, double heightPerLevel, String roofShape,
			Material materialWall, Material materialRoof, boolean hasWindows, boolean hasWalls) {
		this.levels = levels;
		this.heightPerLevel = heightPerLevel;
		this.roofShape = roofShape;
		this.materialWall = materialWall;
		this.materialRoof = materialRoof;
		this.hasWindows = hasWindows;
		this.hasWalls = hasWalls;
	}

	public static BuildingDefaults getDefaultsFor(TagSet tags) {

		String type = tags.getValue("building:part");
		if (type == null || "yes".equals(type)) {
			type = tags.getValue("building");
		}

		if (type == null) {
			throw new IllegalArgumentException("Tags do not contain a building type: " + tags);
		}

		/* determine defaults for building type */

		int levels = 3;
		double heightPerLevel = 2.5;
		Material materialWall = Materials.BUILDING_DEFAULT;
		Material materialRoof = Materials.ROOF_DEFAULT;
		boolean hasWindows = true;
		boolean hasWalls = true;
		String roofShape = "flat";

		switch (type) {

		case "greenhouse":
			levels = 1;
			materialWall = Materials.GLASS_WALL;
			materialRoof = Materials.GLASS_ROOF;
			hasWindows = false;
			break;

		case "garage":
		case "garages":
			levels = 1;
			materialWall = Materials.CONCRETE;
			materialRoof = Materials.CONCRETE;
			hasWindows = false;
			break;

		case "carport":
			levels = 1;
			materialWall = Materials.CONCRETE;
			materialRoof = Materials.CONCRETE;
			hasWindows = false;
			hasWalls = false;
			break;

		case "hut":
		case "shed":
			levels = 1;
			break;

		case "cabin":
			levels = 1;
			materialWall = Materials.WOOD_WALL;
			materialRoof = Materials.WOOD;
			break;

		case "roof":
			levels = 1;
			hasWindows = false;
			hasWalls = false;
			break;

		case "church":
		case "hangar":
		case "industrial":
			hasWindows = false;
			break;

		}

		/* handle other tags */

		if (tags.contains("parking", "multi-storey")) {
			levels = 5;
			hasWindows = false;
		}

		if (tags.contains("man_made", "chimney") || type.equals("chimney")) {
			roofShape = "chimney";
			levels = 1;
			heightPerLevel = 10;
			hasWindows = false;
			materialWall = Materials.BRICK;
			materialRoof = Materials.BRICK;
		}

		/* make flat roofs use concrete by default */

		if (tags.contains("roof:shape", "flat") && materialRoof == Materials.ROOF_DEFAULT) {
			materialRoof = Materials.CONCRETE;
		}

		/* return an object populated with the results */

    	return new BuildingDefaults(levels, heightPerLevel, roofShape,
    			materialWall, materialRoof, hasWindows, hasWalls);

	}

}
