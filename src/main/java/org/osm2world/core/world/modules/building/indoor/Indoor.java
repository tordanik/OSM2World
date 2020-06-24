package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.world.modules.building.BuildingPart;


import java.util.ArrayList;
import java.util.List;

import static org.osm2world.core.util.ValueParseUtil.parseLevels;

public class Indoor implements Renderable {

    private final List<MapElement> elements;

    private BuildingPart buildingPart;


    private List<IndoorWall> walls = new ArrayList<>();
    private List<IndoorRoom> rooms = new ArrayList<>();
    private List<IndoorArea> areas = new ArrayList<>();
    private List<Corridor> corridors = new ArrayList<>();

    public Indoor(List<MapElement> indoorElements, BuildingPart buildingPart){

        /* check all elements are within height limits of building part */

        List<MapElement> tempElements = new ArrayList<>();

        for (MapElement element : indoorElements){

            if (element.getTags().containsKey("level")){

                List<Integer> levels =  parseLevels(element.getTags().getValue("level"));

                if (!levels.isEmpty()){

                    //TODO handle elements that span building parts

                    if(levels.get(0) >= buildingPart.getMinLevel() && levels.get(levels.size() - 1) < buildingPart.getBuildingLevels()){
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

            IndoorObjectData data = new IndoorObjectData(buildingPart, element);

            switch (element.getTags().getValue("indoor")){

                case "wall":

                    walls.add(new IndoorWall(data));
                    break;

                case "room":

                    rooms.add(new IndoorRoom(data));
                    break;

                case "area":

                    areas.add(new IndoorArea(data));
                    break;

                case "corridor":

                    corridors.add(new Corridor(data));
                    break;

            }


        }

    }

    @Override
    public void renderTo(Target target) {

        walls.forEach(w -> w.renderTo(target));

        rooms.forEach(r -> r.renderTo(target));

        areas.forEach(a -> a.renderTo(target));

        corridors.forEach(c -> c.renderTo(target));

    }
}
