package org.osm2world.world.modules.building.roof;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.world.modules.building.BuildingPart;

public class ConeRoof extends PyramidalRoof {

	public ConeRoof(@Nullable BuildingPart buildingPart, PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		super(buildingPart, originalPolygon, tags, material.makeSmooth());
	}

}