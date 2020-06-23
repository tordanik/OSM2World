package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.modules.building.BuildingPart;

import java.util.List;

import static org.osm2world.core.util.ValueParseUtil.parseLevels;

public final class IndoorObjectData {

    private final BuildingPart buildingPart;
    private final MapElement mapElement;
    private final List<Integer> levels;
    private final TagSet tags;
    private final Float levelHeightAboveBase;

    public IndoorObjectData(BuildingPart buildingPart, MapElement mapElement){

        this.buildingPart = buildingPart;
        this.mapElement = mapElement;
        this.levels = parseLevels(mapElement.getTags().getValue("level"));
        this.tags = mapElement.getTags();
        this.levelHeightAboveBase = (float) buildingPart.getLevelHeightAboveBase(getMinLevel());

    }

    public BuildingPart getBuildingPart() { return buildingPart; }

    public Float getLevelHeightAboveBase() { return levelHeightAboveBase; }

    public Double getLevelFloorHeight() { return buildingPart.calculateFloorHeight() + getLevelHeightAboveBase(); }

    public List<Integer> getLevels() { return levels; }

    public Integer getMinLevel(){ return levels.get(0); }

    public Integer getMaxLevel(){ return levels.get(levels.size() - 1); }

    public MapElement getMapElement() { return mapElement; }

    public TagSet getTags() { return tags; }

    // returns height of lowest level
    public Double getLevelHeight() { return buildingPart.getLevelHeight(getMinLevel()); }

    public PolygonWithHolesXZ getPolygon() {
        if ( mapElement instanceof MapArea){
            return ((MapArea) mapElement).getPolygon();
        } else {
            return null;
        }
    }

    public Material getMaterial(Material defaultMaterial){
        return  BuildingPart.buildMaterial(mapElement.getTags().getValue("material"), null, defaultMaterial, false);
    }

}


