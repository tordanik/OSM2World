package org.osm2world.core.world.modules.building.indoor;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.modules.building.BuildingPart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.osm2world.core.util.ValueParseUtil.parseLevels;

public final class IndoorObjectData {

    private final BuildingPart buildingPart;
    private final MapElement mapElement;
    private final List<Integer> levels;
    private List<Integer> renderableLevels;
    private final TagSet tags;
    private final Float levelHeightAboveBase;

    public IndoorObjectData(BuildingPart buildingPart, MapElement mapElement){

        this.buildingPart = buildingPart;
        this.mapElement = mapElement;
        Configuration config = buildingPart.getConfig();
        this.levels = parseLevels(mapElement.getTags().getValue("level"));

        if (config.getString("renderLevels") != null){
            List<String> renLevels = new ArrayList<>(Arrays.asList(config.getString("renderLevels").split(",")));
            this.renderableLevels = parseLevels(mapElement.getTags().getValue("level")).stream().filter(i -> renLevels.contains(Integer.toString(i))).collect(Collectors.toList());
        } else {
            this.renderableLevels = this.levels;
        }

        if(config.getString("notRenderLevels") != null){
            List<String> notRenLevels = new ArrayList<>(Arrays.asList(config.getString("notRenderLevels").split(",")));
            this.renderableLevels = renderableLevels.stream().filter(i -> !notRenLevels.contains(Integer.toString(i))).collect(Collectors.toList());
        }

        this.tags = mapElement.getTags();
        this.levelHeightAboveBase = (float) buildingPart.getLevelHeightAboveBase(getMinLevel());
    }

    public BuildingPart getBuildingPart() { return buildingPart; }

    // height of lowest level above base
    public Float getLevelHeightAboveBase() { return levelHeightAboveBase; }

    public List<Integer> getLevels() { return levels; }

    public List<Integer> getRenderableLevels() { return renderableLevels; }

    public Integer getMinLevel(){ return levels.get(0); }

    public Integer getMaxLevel(){ return levels.get(levels.size() - 1); }

    // returns the highest point of an object based on its max level
    public Double getTopOfTopLevelHeightAboveBase(){ return buildingPart.getLevelHeightAboveBase(getMaxLevel()) + buildingPart.getLevelHeight(getMaxLevel()); }

    public MapElement getMapElement() { return mapElement; }

    public TagSet getTags() { return tags; }

    // returns height of lowest level e.g. floor to ceiling
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


