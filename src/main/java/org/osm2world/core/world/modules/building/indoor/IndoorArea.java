package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.world.attachment.AttachmentSurface;

import java.util.Collection;

public class IndoorArea implements Renderable {

    private final IndoorFloor floor;

    private final IndoorObjectData data;

    IndoorArea(IndoorObjectData data){

        this.data = data;

        PolygonWithHolesXZ polygon = data.getPolygon();
        double floorHeight = (double) data.getLevelHeightAboveBase();

        /* allow for transparent windows for adjacent objects */
        if (data.getMapElement() instanceof MapArea) {
            data.getLevels().forEach(l -> data.getBuildingPart().getBuilding().addListWindowNodes(((MapArea) data.getMapElement()).getBoundaryNodes(), l));
        }

        floor = new IndoorFloor(data.getBuildingPart(), data.getSurface(), polygon, floorHeight,
                data.getRenderableLevels().contains(data.getMinLevel()), data.getMinLevel());
    }

    public Collection<AttachmentSurface> getAttachmentSurfaces() {
        return floor.getAttachmentSurfaces();
    }

    @Override
    public void renderTo(Target target) {
        floor.renderTo(target);
    }
}
