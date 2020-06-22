package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.world.modules.building.BuildingPart;


import java.util.ArrayList;
import java.util.List;

import static org.osm2world.core.util.ValueParseUtil.parseOsmDecimal;

public class Indoor implements Renderable {

    private final List<MapElement> elements;

    private BuildingPart buildingPart;


    private List<IndoorWall> walls = new ArrayList<>();
    private List<IndoorRoom> rooms = new ArrayList<>();
    private List<IndoorArea> areas = new ArrayList<>();

    public Indoor(List<MapElement> indoorElements, BuildingPart buildingPart){

        /* check all elements are within height limits of building part */

        List<MapElement> tempElements = new ArrayList<>();

        for (MapElement element : indoorElements){

            TagSet elementTags = element.getTags();

            if (elementTags.containsKey("level")){

                Float elementLevel = parseOsmDecimal(elementTags.getValue("level"), true);

                if (elementLevel != null){

                    if(elementLevel >= buildingPart.getMinLevel() && elementLevel <= buildingPart.getBuildingLevels()){
                        tempElements.add(element);
                    }

                }

            }

            //TODO handle element with no level tag

        }

        this.elements = tempElements;
        this.buildingPart = buildingPart;

        createComponents();

    }

    private void createComponents(){

        for (MapElement element : elements){
            if (element.getTags().contains("indoor", "wall")){

                walls.add(new IndoorWall(buildingPart, element));

            } else if (element.getTags().contains("indoor", "room")){

                rooms.add(new IndoorRoom((MapArea) element, buildingPart));

            } else if (element.getTags().contains("indoor", "area")){

                areas.add(new IndoorArea(buildingPart, (MapArea) element));

            }
        }

    }

    @Override
    public void renderTo(Target target) {

        walls.forEach(w -> w.renderTo(target));

        rooms.forEach(r -> r.renderTo(target));

        areas.forEach(a -> a.renderTo(target));

    }
}
