package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.modules.building.Floor;

public class IndoorArea implements Renderable {

    private final Floor floor;
    private final Ceiling ceiling;

    private final IndoorObjectData data;

    public IndoorArea(IndoorObjectData data){
        Material material = data.getMaterial(Materials.WOOD_WALL);
        PolygonWithHolesXZ polygon = data.getPolygon();
        Double floorHeight = data.getLevelFloorHeight();

        this.data = data;

        floor = new Floor(data.getBuildingPart(), material, polygon, floorHeight);
        ceiling = new Ceiling(data.getBuildingPart(), material, polygon, floorHeight);
    }

    @Override
    public void renderTo(Target target) {

        floor.renderTo(target);
        ceiling.renderTo(target);

    }
}
