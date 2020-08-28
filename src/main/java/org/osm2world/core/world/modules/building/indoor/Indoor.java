package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.world.attachment.AttachmentSurface;
import org.osm2world.core.world.modules.building.BuildingPart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Indoor implements Renderable {

    private final List<MapElement> elements;

    private BuildingPart buildingPart;

    private List<IndoorWall> walls = new ArrayList<>();
    private List<IndoorRoom> rooms = new ArrayList<>();
    private List<IndoorArea> areas = new ArrayList<>();
    private List<Corridor> corridors = new ArrayList<>();

    List<AttachmentSurface> surfaces = null;

    public Indoor(List<MapElement> indoorElements, BuildingPart buildingPart){

        this.elements = indoorElements;
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

    public Collection<AttachmentSurface> getAttachmentSurfaces() {

        if (surfaces == null) {

            surfaces = new ArrayList<>();

            for (IndoorRoom room : rooms) {
                surfaces.addAll(room.getAttachmentSurfaces());
            }

            for (IndoorArea area : areas) {
                surfaces.addAll(area.getAttachmentSurfaces());
            }

            for (Corridor corridor : corridors) {
                surfaces.addAll(corridor.getAttachmentSurfaces());
            }

            for (IndoorWall wall : walls) {
                surfaces.addAll(wall.getAttachmentSurfaces());
            }

        }

        return surfaces;

    }

    @Override
    public void renderTo(Target target) {

        IndoorWall.allRenderedWallSegments = new ArrayList<>();

        walls.forEach(w -> w.renderTo(target));

        rooms.forEach(r -> r.renderTo(target));

        areas.forEach(a -> a.renderTo(target));

        corridors.forEach(c -> c.renderTo(target));

    }
}
