package org.osm2world.world.modules.building.roof;

import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.scene.material.Material;

public class ConeRoof extends PyramidalRoof {

	public ConeRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		super(originalPolygon, tags, material.makeSmooth());
	}

}