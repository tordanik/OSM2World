package org.osm2world.world.modules.building.indoor;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.osm2world.util.ValueParseUtil.parseLevels;

import java.util.List;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.MapElement;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.shapes.PolygonWithHolesXZ;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.MaterialOrRef;
import org.osm2world.world.modules.building.BuildingDefaults;
import org.osm2world.world.modules.building.BuildingPart;

public final class IndoorObjectData {

    private final BuildingPart buildingPart;
    private final MapElement mapElement;
    private final List<Integer> levels;
    private List<Integer> renderableLevels;
    private final TagSet tags;
    private final double levelHeightAboveBase;

    public IndoorObjectData(BuildingPart buildingPart, MapElement mapElement){

        this.buildingPart = buildingPart;
        this.mapElement = mapElement;

        this.levels = parseLevels(mapElement.getTags().getValue("level"), emptyList());

        O2WConfig config = buildingPart.getConfig();

        if (config.getString("renderLevels") != null){
            List<String> renLevels = asList(config.getString("renderLevels").split(","));
            this.renderableLevels = levels.stream()
                    .filter(i -> renLevels.contains(Integer.toString(i)))
                    .collect(toList());
        } else {
            this.renderableLevels = this.levels;
        }

        if(config.getString("notRenderLevels") != null){
            List<String> notRenLevels = asList(config.getString("notRenderLevels").split(","));
            this.renderableLevels = renderableLevels.stream()
                    .filter(i -> !notRenLevels.contains(Integer.toString(i)))
                    .collect(toList());
        }

        this.tags = mapElement.getTags();
        this.levelHeightAboveBase = (float) buildingPart.levelStructure.level(getMinLevel()).relativeEle;

    }

    public BuildingPart getBuildingPart() { return buildingPart; }

    /**
     * @return height of lowest level above base
     */
    public double getLevelHeightAboveBase() { return levelHeightAboveBase; }

    public List<Integer> getLevels() { return levels; }

    public List<Integer> getRenderableLevels() { return renderableLevels; }

    public Integer getMinLevel(){ return levels.get(0); }

    public Integer getMaxLevel(){ return levels.get(levels.size() - 1); }

    /**
     * @return the highest point of an object based on its max level
     */
    public Double getTopOfTopLevelHeightAboveBase(){
        return buildingPart.levelStructure.level(getMaxLevel()).relativeEleTop();
    }

    public MapElement getMapElement() { return mapElement; }

    public TagSet getTags() { return tags; }

    public PolygonWithHolesXZ getPolygon() {
        if (mapElement instanceof MapArea){
            return ((MapArea) mapElement).getPolygon();
        } else {
            return null;
        }
    }

    public Material getMaterial(MaterialOrRef defaultMaterial, O2WConfig config) {
        return BuildingPart.buildMaterial(mapElement.getTags().getValue("material"), null, defaultMaterial, false, config);
    }

    public Material getSurface(O2WConfig config) {
        if (tags.containsKey("surface")) {
            return BuildingPart.buildMaterial(tags.getValue("surface"), null, BuildingDefaults.getDefaultsFor(buildingPart.getTags()).materialWall, false, config);
        } else {
            return BuildingDefaults.getDefaultsFor(buildingPart.getTags()).materialWall.get(config);
        }
    }

}


