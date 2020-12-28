package org.osm2world.core.world.modules.building.indoor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.attachment.AttachmentSurface;
import org.osm2world.core.world.modules.building.BuildingDefaults;

public class Corridor implements Renderable {

    private final IndoorFloor floor;
    private final Ceiling ceiling;

    private final IndoorObjectData data;

    Corridor(IndoorObjectData data){

        this.data = data;

        Material material = data.getMaterial(BuildingDefaults.getDefaultsFor(data.getBuildingPart().getTags()).materialWall);
        PolygonWithHolesXZ polygon = data.getPolygon();
        double floorHeight = data.getLevelHeightAboveBase();

        /* allow for transparent windows for adjacent objects */
        if (data.getMapElement() instanceof MapArea) {
            data.getLevels().forEach(l -> data.getBuildingPart().getBuilding().addListWindowNodes(((MapArea) data.getMapElement()).getBoundaryNodes(), l));
        }

        floor = new IndoorFloor(data.getBuildingPart(), data.getSurface(), polygon, floorHeight,
                data.getRenderableLevels().contains(data.getMinLevel()), data.getMinLevel());
        ceiling = new Ceiling(data.getBuildingPart(), material, polygon, data.getTopOfTopLevelHeightAboveBase(),
                data.getRenderableLevels().contains(data.getMaxLevel()), data.getMaxLevel());
    }

    public Collection<AttachmentSurface> getAttachmentSurfaces() {

        Collection<AttachmentSurface> floorSurfaces = floor.getAttachmentSurfaces();
        Collection<AttachmentSurface> ceilingSurfaces = ceiling.getAttachmentSurfaces();

        List<AttachmentSurface> surfaces = new ArrayList<>();

        surfaces.addAll(floorSurfaces);
        surfaces.addAll(ceilingSurfaces);

        return surfaces;

    }

    @Override
    public void renderTo(Target target){

        floor.renderTo(target);
        ceiling.renderTo(target);

    }
}
