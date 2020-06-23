package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.modules.building.BuildingPart;

public class Corridor implements Renderable {

    private IndoorFloor floor;
    private Ceiling ceiling;

    private final IndoorObjectData data;

    public Corridor(IndoorObjectData data){

        this.data = data;

        Material material = data.getMaterial(BuildingPart.createWallMaterial(data.getBuildingPart().getTags(), data.getBuildingPart().getConfig()));
        PolygonWithHolesXZ polygon = data.getPolygon();
        Double floorHeight = data.getLevelFloorHeight();

        floor = new IndoorFloor(data.getBuildingPart(), material, polygon, floorHeight);
        ceiling = new Ceiling(data.getBuildingPart(), material, polygon, floorHeight + data.getLevelHeight());
    }

    @Override
    public void renderTo(Target target) {

        floor.renderTo(target);
        ceiling.renderTo(target);

    }
}
