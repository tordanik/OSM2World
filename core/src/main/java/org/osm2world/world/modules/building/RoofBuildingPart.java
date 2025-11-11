package org.osm2world.world.modules.building;

import static org.osm2world.scene.material.Materials.GLASS_WALL;
import static org.osm2world.scene.material.Materials.getTransparentVariant;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.scene.material.Material;

/**
 * a special kind of building part representing a roof without walls
 */
public class RoofBuildingPart extends BuildingPart {

	public RoofBuildingPart(Building building, MapArea area, O2WConfig config) {
		super(building, area, config);
	}

	/** whether this roof is at least partially transparent */
	private boolean isTransparent() {
		return getTransparentVariant(super.createWallMaterial(tags, config)) != null
				|| getTransparentVariant(super.createRoofMaterial(tags, config)) != null;
	}

	@Override
	protected boolean isBottomless() {
		return isTransparent();
	}

	@Override
	protected Material createWallMaterial(TagSet tags, O2WConfig config) {

		Material result = super.createWallMaterial(tags, config);

		if (!tags.containsKey("building:material") && tags.containsKey("roof:material")) {
			// use a roof-dependent default material for the small bit of "wall" below the roof
			if ("glass".equals(tags.getValue("roof:material"))) {
				result = GLASS_WALL.get();
			}
		}

		return modifyIfTransparent(result);

	}

	@Override
	protected Material createRoofMaterial(TagSet tags, O2WConfig config) {
		return modifyIfTransparent(super.createRoofMaterial(tags, config));
	}

	private Material modifyIfTransparent(Material material) {

		Material result = material;

		if (isTransparent()) {

			Material transparentMaterial = getTransparentVariant(result);
			if (transparentMaterial != null) {
				result = transparentMaterial;
			}

			result = result.makeDoubleSided();

		}

		return result;

	}

}
