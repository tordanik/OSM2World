package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.map_data.data.*;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.world.modules.building.BuildingPart;
import org.osm2world.core.world.modules.building.Floor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



import static org.osm2world.core.util.ValueParseUtil.parseOsmDecimal;

public class IndoorRoom implements Renderable {

    private final Float levelHeight;
    private final Float bottomOfLevelHeight;
    private final int level;

    private final BuildingPart buildingPart;

    private final MapArea roomArea;

    private final List<IndoorWall> walls;
    private final Floor floor;
    private final Ceiling ceiling;

    public IndoorRoom(MapArea room, BuildingPart buildingPart){

        this.roomArea = room;

        //TODO move this into immutable class
        this.buildingPart = buildingPart;

        this.level = ((int)((double)parseOsmDecimal(room.getTags().getValue("level"), false)));
        this.levelHeight = (float) buildingPart.getLevelHeight(level);
        this.bottomOfLevelHeight = (float) buildingPart.getLevelHeightAboveBase(level);


        this.walls = splitIntoIndoorWalls();

        floor = new Floor(buildingPart, BuildingPart.createWallMaterial(buildingPart.getTags(), buildingPart.getConfig()),room.getPolygon(), bottomOfLevelHeight + levelHeight);
        ceiling = new Ceiling(buildingPart, BuildingPart.createWallMaterial(buildingPart.getTags(), buildingPart.getConfig()),room.getPolygon(), bottomOfLevelHeight + 0.01);
    }

    private List<IndoorWall> splitIntoIndoorWalls(){

        List<IndoorWall> result = new ArrayList<>();

        result.add(new IndoorWall(buildingPart, roomArea));

        return result;
    }



    @Override
    public void renderTo(Target target) {

        walls.forEach(w -> w.renderTo(target));

        floor.renderTo(target);

        ceiling.renderTo(target);

    }
}

