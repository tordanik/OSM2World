package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.map_data.data.*;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.modules.building.BuildingPart;
import org.osm2world.core.world.modules.building.Wall;
import org.osm2world.core.world.modules.building.WallSurface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.VectorXZ.listXYZ;
import static org.osm2world.core.util.ValueParseUtil.parseOsmDecimal;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.inheritTags;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseHeight;

public class IndoorWall implements Renderable {

    private final Float wallHeight;
    private final Float floorHeight;

    private List<LineSegmentXZ> wallSegments = new ArrayList<>();

    private final IndoorObjectData data;


    //TODO multilevel
    //TODO underground
    public IndoorWall(IndoorObjectData objectData){

        this.data = objectData;

        if (parseHeight(data.getMapElement().getTags(), -1) > 0){
            this.wallHeight = parseHeight(data.getMapElement().getTags(), -1);
        } else {
            //TODO change to account for multiple levels
            this.wallHeight = (float) data.getBuildingPart().getLevelHeight(data.getMinLevel());
        }

        List<VectorXZ> points = ((MapWaySegment) data.getMapElement()).getWay().getNodes().stream().map(MapNode::getPos).collect(toList());

        for (int i = 0; i < points.size() - 1; i++){
            wallSegments.add(new LineSegmentXZ(points.get(i), points.get(i + 1)));
        }


        this.floorHeight = data.getBuildingPart() == null ? 0 : (float) data.getBuildingPart().calculateFloorHeight();

    }

    public IndoorWall(BuildingPart buildingPart, MapArea area){

        data = new IndoorObjectData(buildingPart, area);

        wallSegments = area.getPolygon().getSegments();

        if (parseHeight(area.getTags(), -1) > 0){
            this.wallHeight = parseHeight(area.getTags(), -1);
        } else {
            this.wallHeight = (float) buildingPart.getLevelHeight(data.getMinLevel());
        }

        this.floorHeight = (float) buildingPart.calculateFloorHeight();

    }

    @Override
    public void renderTo(Target target) {

        double baseEle = data.getBuildingPart().getBuilding().getGroundLevelEle();

        Material material =  BuildingPart.buildMaterial(data.getTags().getValue("material"), null, Materials.BRICK,false);


        for (LineSegmentXZ wallSeg : wallSegments){

            List<VectorXZ> vectors = wallSeg.getVertexList();

            /* front wall surface */

            List<VectorXYZ> bottomPoints = new ArrayList<>(listXYZ(vectors, baseEle + data.getLevelHeightAboveBase()));
            List<VectorXYZ> topPoints = new ArrayList<>(listXYZ(vectors, baseEle + data.getLevelHeightAboveBase() + wallHeight));

            WallSurface mainSurface = new WallSurface(material, bottomPoints, topPoints);

            /* back wall surface */

            List<VectorXYZ> backBottomPoints = new ArrayList<>(bottomPoints);
            List<VectorXYZ> backTopPoints = new ArrayList<>(topPoints);

            Collections.reverse(backTopPoints);
            Collections.reverse(backBottomPoints);

            WallSurface backSurface = new WallSurface(material, backBottomPoints, backTopPoints);

            /* draw wall */

            if (mainSurface != null) {
                mainSurface.renderTo(target, new VectorXZ(0, -floorHeight), false, 0);
                backSurface.renderTo(target, new VectorXZ(0, -floorHeight), false, 0);
            }

        }

    }

}
