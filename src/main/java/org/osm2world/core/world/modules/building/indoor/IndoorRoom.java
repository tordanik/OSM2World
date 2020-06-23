package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.map_data.data.*;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.world.modules.building.BuildingPart;
import org.osm2world.core.world.modules.building.Floor;

import java.util.ArrayList;
import java.util.List;

public class IndoorRoom implements Renderable {

    private final MapArea roomArea;

    private final List<IndoorWall> walls;
    private final Floor floor;
    private final Ceiling ceiling;

    private final IndoorObjectData data;

    public IndoorRoom(IndoorObjectData data){

        this.data = data;

        this.roomArea = (MapArea) data.getMapElement();

        this.walls = splitIntoIndoorWalls();

        floor = new Floor(data.getBuildingPart(), BuildingPart.createWallMaterial(data.getBuildingPart().getTags(), data.getBuildingPart().getConfig()), data.getPolygon(), data.getLevelFloorHeight() + data.getLevelHeight());
        ceiling = new Ceiling(data.getBuildingPart(), BuildingPart.createWallMaterial(data.getBuildingPart().getTags(), data.getBuildingPart().getConfig()), data.getPolygon(), data.getLevelFloorHeight() + 0.01);
    }

    private List<IndoorWall> splitIntoIndoorWalls(){

        List<IndoorWall> result = new ArrayList<>();

        result.add(new IndoorWall(data.getBuildingPart(), roomArea));

        return result;
    }



    @Override
    public void renderTo(Target target) {

        walls.forEach(w -> w.renderTo(target));

        floor.renderTo(target);

        ceiling.renderTo(target);

    }
}

