package org.osm2world.core.world.modules.building.indoor;

import static java.util.Collections.emptyList;

import java.util.Collection;

import org.apache.commons.lang.math.IntRange;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.world.attachment.AttachmentSurface;
import org.osm2world.core.world.data.AreaWorldObject;
import org.osm2world.core.world.data.ProceduralWorldObject;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.modules.building.BuildingDefaults;

public class IndoorRoom implements AreaWorldObject, ProceduralWorldObject {

    private final IndoorWall wall;
    private final IndoorFloor floor;
    private final Ceiling ceiling;

    private final IndoorObjectData data;

    IndoorRoom(IndoorObjectData data){

    	((MapArea) data.getMapElement()).addRepresentation(this);

        this.data = data;

        this.wall = new IndoorWall(data.getBuildingPart(), data.getMapElement());

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

    }

    public Collection<AttachmentSurface> getAttachmentSurfaces() {
        return floor.getAttachmentSurfaces();
    }

	@Override
	public void buildMeshesAndModels(Target target) {

        wall.renderTo(target);

        floor.renderTo(target);

        ceiling.renderTo(target);

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

	public IntRange getLevelRange() {
		return new IntRange(floor.level, ceiling.level);
	}

}

