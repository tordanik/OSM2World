package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.modules.building.BuildingPart;
import org.osm2world.core.world.modules.building.Wall;
import org.osm2world.core.world.modules.building.WallSurface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.osm2world.core.math.VectorXZ.listXYZ;
import static org.osm2world.core.util.ValueParseUtil.parseOsmDecimal;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseHeight;

public class IndoorWall extends Wall {

    private final Float wallHeight;
    private final Float bottomOfLevelHeight;
    private final int level;

    private List<LineSegmentXZ> wallSegments = new ArrayList<>();

    //TODO multilevel
    //TODO underground
    public IndoorWall(MapElement segment, BuildingPart buildingPart){
        super(((MapWaySegment) segment).getWay(), buildingPart, ((MapWaySegment) segment).getWay().getNodes());

        this.level =  ((int)((double)parseOsmDecimal(segment.getTags().getValue("level"), false)));

        if (parseHeight(segment.getTags(), -1) > 0){
            this.wallHeight = parseHeight(segment.getTags(), -1);
        } else {
            this.wallHeight = (float) buildingPart.getLevelHeight(level);
        }

        this.bottomOfLevelHeight = (float) buildingPart.getLevelHeightAboveBase(level);

        List<VectorXZ> points = getPoints().getVertexList();

        for (int i = 0; i < points.size() - 1; i++){
            wallSegments.add(new LineSegmentXZ(points.get(i), points.get(i + 1)));
        }

    }

    @Override
    public void renderTo(Target target) {

        double baseEle = getBuildingPart().getBuilding().getGroundLevelEle();

        Material material = BuildingPart.createWallMaterial(getTags(), getBuildingPart().getConfig());

        for (LineSegmentXZ wallSeg : wallSegments){

            List<VectorXZ> vectors = wallSeg.getVertexList();

            /* front wall surface */

            List<VectorXYZ> bottomPoints = new ArrayList<>(listXYZ(vectors, baseEle + bottomOfLevelHeight));
            List<VectorXYZ> topPoints = new ArrayList<>(listXYZ(vectors, baseEle + bottomOfLevelHeight + wallHeight));

            WallSurface mainSurface = new WallSurface(material, bottomPoints, topPoints);

            /* back wall surface */

            List<VectorXYZ> backBottomPoints = new ArrayList<>(bottomPoints);
            List<VectorXYZ> backTopPoints = new ArrayList<>(topPoints);

            Collections.reverse(backTopPoints);
            Collections.reverse(backBottomPoints);

            WallSurface backSurface = new WallSurface(material, backBottomPoints, backTopPoints);

            /* draw wall */

            if (mainSurface != null) {
                mainSurface.renderTo(target, new VectorXZ(0, -getFloorHeight()), false, 0);
                backSurface.renderTo(target, new VectorXZ(0, -getFloorHeight()), false, 0);
            }

        }

    }

}
