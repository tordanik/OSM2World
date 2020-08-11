package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.world.attachment.AttachmentSurface;
import org.osm2world.core.world.modules.building.BuildingDefaults;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IndoorRoom implements Renderable {

    private final List<IndoorWall> walls;
    private final IndoorFloor floor;
    private final Ceiling ceiling;

    private final IndoorObjectData data;

    public IndoorRoom(IndoorObjectData data){

        this.data = data;

        this.walls = splitIntoIndoorWalls();

        floor = new IndoorFloor(data.getBuildingPart(),
                data.getSurface(),
                data.getPolygon(),
                data.getLevelHeightAboveBase(),
                data.getRenderableLevels().contains(data.getMinLevel()), data.getMinLevel());

        ceiling = new Ceiling(data.getBuildingPart(),
                data.getMaterial(BuildingDefaults.getDefaultsFor(data.getBuildingPart().getTags()).materialWall),
                data.getPolygon(),
                data.getTopOfTopLevelHeightAboveBase(),
                data.getRenderableLevels().contains(data.getMaxLevel()), data.getMaxLevel());

        if (data.getMapElement() instanceof MapArea) {
            data.getLevels().forEach(l ->  data.getBuildingPart().getBuilding().addListWindowNodes(((MapArea) data.getMapElement()).getBoundaryNodes(), l));
        }

    }

    public Collection<AttachmentSurface> getAttachmentSurfaces() {

        Collection<AttachmentSurface> floorSurfaces = floor.getAttachmentSurfaces();
        Collection<AttachmentSurface> ceilingSurfaces = ceiling.getAttachmentSurfaces();

        List<AttachmentSurface> surfaces = new ArrayList<>();

        surfaces.addAll(floorSurfaces);
        surfaces.addAll(ceilingSurfaces);

        return surfaces;

    }

    private List<IndoorWall> splitIntoIndoorWalls(){

        List<IndoorWall> result = new ArrayList<>();

        result.add(new IndoorWall(data.getBuildingPart(), data.getMapElement()));

        return result;
    }



    @Override
    public void renderTo(Target target) {

        walls.forEach(w -> w.renderTo(target));

        floor.renderTo(target);

        ceiling.renderTo(target);

    }
}

