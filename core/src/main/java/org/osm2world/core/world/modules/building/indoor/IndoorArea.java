package org.osm2world.core.world.modules.building.indoor;

import static java.util.Collections.emptyList;

import java.util.Collection;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.world.attachment.AttachmentSurface;
import org.osm2world.core.world.data.AreaWorldObject;
import org.osm2world.core.world.data.ProceduralWorldObject;
import org.osm2world.core.world.data.WorldObject;

public class IndoorArea implements AreaWorldObject, ProceduralWorldObject {

    private final IndoorFloor floor;

    private final IndoorObjectData data;

    IndoorArea(IndoorObjectData data){

    	((MapArea) data.getMapElement()).addRepresentation(this);

        this.data = data;

        PolygonWithHolesXZ polygon = data.getPolygon();
        double floorHeight = data.getLevelHeightAboveBase();

        floor = new IndoorFloor(data.getBuildingPart(), data.getSurface(), polygon, floorHeight,
                data.getRenderableLevels().contains(data.getMinLevel()), data.getMinLevel());
    }

    public Collection<AttachmentSurface> getAttachmentSurfaces() {
        return floor.getAttachmentSurfaces();
    }

	@Override
	public void buildMeshesAndModels(Target target) {
		floor.renderTo(target);
    }

	@Override
	public MapArea getPrimaryMapElement() {
		return (MapArea) data.getMapElement();
	}

	@Override
	public WorldObject getParent() {
		return data.getBuildingPart();
	}

	@Override
	public Iterable<EleConnector> getEleConnectors() {
		return emptyList();
	}

	public int getFloorLevel() {
		return floor.level;
	}

}
