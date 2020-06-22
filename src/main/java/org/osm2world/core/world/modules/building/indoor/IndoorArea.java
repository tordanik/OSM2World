package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.modules.building.BuildingPart;
import org.osm2world.core.world.modules.building.Floor;

import static org.osm2world.core.util.ValueParseUtil.parseOsmDecimal;

public class IndoorArea implements Renderable {

    private final Floor floor;
    private final Ceiling ceiling;

    public IndoorArea(BuildingPart buildingPart, MapArea area){
        Material material = BuildingPart.buildMaterial(area.getTags().getValue("material"), null, Materials.WOOD_WALL, false);
        PolygonWithHolesXZ polygon = area.getPolygon();
        Double floorHeight = buildingPart.getLevelHeightAboveBase(((int)((double)parseOsmDecimal(area.getTags().getValue("level"), false))));

        floor = new Floor(buildingPart, material, polygon, floorHeight);
        ceiling = new Ceiling(buildingPart, material, polygon, floorHeight);
    }

    @Override
    public void renderTo(Target target) {

        floor.renderTo(target);
        ceiling.renderTo(target);

    }
}
