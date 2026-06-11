package org.osm2world.world.modules.building.roof;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.world.modules.building.BuildingPart;

public class HippedRoof extends AbstractHippedRoof {

	static final double RIDGE_OFFSET = 1/3.0;

	public HippedRoof(@Nullable BuildingPart buildingPart, PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		super(buildingPart, RIDGE_OFFSET, RIDGE_OFFSET, false, originalPolygon, tags, material);
	}

}
