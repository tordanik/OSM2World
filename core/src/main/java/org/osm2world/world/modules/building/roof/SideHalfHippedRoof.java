package org.osm2world.world.modules.building.roof;

import static org.osm2world.world.modules.building.roof.HalfHippedRoof.RIDGE_OFFSET;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.world.modules.building.BuildingPart;

public class SideHalfHippedRoof extends AbstractHalfHippedRoof {

	public SideHalfHippedRoof(@Nullable BuildingPart buildingPart, PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		super(buildingPart, 0, RIDGE_OFFSET, true, originalPolygon, tags, material);
	}

	@Override
	protected @Nullable VectorXZ defaultDirection() {
		if (buildingPart == null) return null;
		return SideHippedRoof.determineDefaultDirection(buildingPart.getPrimaryMapElement());
	}

}
