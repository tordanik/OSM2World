package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.modules.building.BuildingDefaults;

public class IndoorArea implements Renderable {

    private final IndoorFloor floor;
    private final Ceiling ceiling;

    private final IndoorObjectData data;

    public IndoorArea(IndoorObjectData data){

        this.data = data;

        Material material = data.getMaterial(BuildingDefaults.getDefaultsFor(data.getBuildingPart().getTags()).materialWall);
        PolygonWithHolesXZ polygon = data.getPolygon();
        Double floorHeight = (double) data.getLevelHeightAboveBase();

        if (data.getMapElement() instanceof MapArea) {
            data.getBuildingPart().getBuilding().addListWindowNodes(((MapArea) data.getMapElement()).getBoundaryNodes(), data.getMinLevel());
        }

        floor = new IndoorFloor(data.getBuildingPart(), data.getSurface(), polygon, floorHeight, data.getRenderableLevels().contains(data.getMinLevel()));
        ceiling = new Ceiling(data.getBuildingPart(), material, polygon, floorHeight, data.getRenderableLevels().contains(data.getMinLevel()));
    }

    @Override
    public void renderTo(Target target) {

        floor.renderTo(target);
        ceiling.renderTo(target);

    }
}
