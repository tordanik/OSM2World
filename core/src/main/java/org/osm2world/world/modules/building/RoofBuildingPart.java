package org.osm2world.world.modules.building;

import static org.osm2world.scene.material.DefaultMaterials.GLASS_WALL;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.MaterialOrRef;

/**
 * a special kind of building part representing a roof without walls
 */
public class RoofBuildingPart extends BuildingPart {

	public RoofBuildingPart(Building building, MapArea area, O2WConfig config) {
		super(building, area, config);
	}

	/** whether this roof is at least partially transparent */
	private boolean isTransparent() {
		return config.mapStyle().getTransparentVariant(super.createWallMaterial(tags, config)) != null
				|| config.mapStyle().getTransparentVariant(super.createRoofMaterial(tags, config)) != null;
	}

	@Override
	protected boolean isBottomless() {
		return isTransparent() || isUnwalled();
	}

	@Override
	protected boolean isUnwalled() {
		return getTags().contains("wall", "no");
	}

	@Override
	protected Material createWallMaterial(TagSet tags, O2WConfig config) {

		Material result = super.createWallMaterial(tags, config);

		if (!tags.containsKey("building:material") && tags.containsKey("roof:material")) {
			// use a roof-dependent default material for the small bit of "wall" below the roof
			if ("glass".equals(tags.getValue("roof:material"))) {
				result = GLASS_WALL.get(config);
			}
		}

		return modifyIfTransparentOrTwoSided(result);

	}

	@Override
	protected Material createRoofMaterial(TagSet tags, O2WConfig config) {
		return modifyIfTransparentOrTwoSided(super.createRoofMaterial(tags, config));
	}

	private Material modifyIfTransparentOrTwoSided(MaterialOrRef material) {

		Material result = material.get(config);

		if (isTransparent() || isUnwalled()) {

			if (isTransparent()) {
				Material transparentResult = config.mapStyle().getTransparentVariant(material);
				if (transparentResult != null) {
					result = transparentResult;
				}
			}

			return result.makeDoubleSided();

		}

		return result;

	}

}
