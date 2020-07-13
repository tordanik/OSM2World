package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.map_data.data.*;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.SimplePolygonXZ;
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

    private List<MapNode> nodes;

    private List<LineSegmentXZ> wallSegments = new ArrayList<>();
    static List<SegmentLevelPair> allRenderedWallSegments = new ArrayList<>();

    private final IndoorObjectData data;

    //TODO account for height of wall
    public IndoorWall(IndoorObjectData objectData){

        this.data = objectData;

        this.wallHeight = data.getTopOfTopLevelHeightAboveBase().floatValue();

//        List<VectorXZ> points = new ArrayList<>();
//
//        if (data.getMapElement() instanceof MapWaySegment) {
//             points = ((MapWaySegment) data.getMapElement()).getWay().getNodes()
//                    .stream().map(MapNode::getPos)
//                    .collect(toList());
//        }

        nodes = ((MapWaySegment) data.getMapElement()).getWay().getNodes();

//        for (int i = 0; i < points.size() - 1; i++){
//            wallSegments.add(new LineSegmentXZ(points.get(i), points.get(i + 1)));
//        }

        this.floorHeight = data.getBuildingPart() == null ? 0 : (float) data.getLevelHeightAboveBase();

        splitIntoWalls();

    }

    public IndoorWall(BuildingPart buildingPart, MapElement element){

        data = new IndoorObjectData(buildingPart, element);
        this.floorHeight = (float) buildingPart.calculateFloorHeight();
        this.wallHeight = data.getTopOfTopLevelHeightAboveBase().floatValue();

        if (element instanceof MapArea) {
//            wallSegments = ((MapArea) element).getPolygon().getSegments();
            nodes = ((MapArea) element).getBoundaryNodes();
            splitIntoWalls();
        }


    }

    private boolean isCornerOrEnd(int index) {

        if (index == 0 || index == nodes.size() - 1) {
            return true;
        }

        VectorXZ segmentBefore = nodes.get(index).getPos().subtract(nodes.get(index - 1).getPos());
        VectorXZ segmentAfter = nodes.get(index + 1).getPos().subtract(nodes.get(index).getPos());
        double dot = segmentBefore.normalize().dot(segmentAfter.normalize());
        if (Math.abs(dot - 1) < 0.05) {
            return false;
        }

        return true;
    }

    private void splitIntoWalls(){

        MapNode prevNode = nodes.get(0);

        for (int i = 1; i < nodes.size(); i++) {

            MapNode node = nodes.get(i);

            if (isCornerOrEnd(i)) {
                wallSegments.add(new LineSegmentXZ(prevNode.getPos(), node.getPos()));
                prevNode = node;
            }

        }

    }

    private class SegmentLevelPair {

        LineSegmentXZ segment;
        Integer level;

        SegmentLevelPair(LineSegmentXZ segment, Integer level){
            this.segment = segment;
            this.level = level;
        }

        private Boolean roughlyEquals(LineSegmentXZ seg){
            return (seg.p1.subtract(this.segment.p1).lengthSquared() + seg.p2.subtract(this.segment.p2).lengthSquared() < 0.1)
                    || (seg.p2.subtract(this.segment.p1).lengthSquared() + seg.p1.subtract(this.segment.p2).lengthSquared() < 0.1);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SegmentLevelPair temp = (SegmentLevelPair) o;
            return roughlyEquals(temp.segment) && this.level.equals(temp.level);
        }


    }

    @Override
    public void renderTo(Target target) {

        double baseEle = data.getBuildingPart().getBuildingPartBaseEle();

        Material material = BuildingPart.buildMaterial(data.getTags().getValue("material"), null, Materials.BRICK, false);

        for (Integer level : data.getRenderableLevels()) {

            for (LineSegmentXZ wallSeg : wallSegments) {

                SegmentLevelPair pair = new SegmentLevelPair(wallSeg, level);

                if (!allRenderedWallSegments.contains(pair)) {

                    allRenderedWallSegments.add(pair);

                    List<VectorXZ> vectors = wallSeg.getVertexList();


                    /* front wall surface */

                    List<VectorXYZ> bottomPoints = new ArrayList<>(listXYZ(vectors,
                            baseEle + data.getBuildingPart().getLevelHeightAboveBase(level)));

                    List<VectorXYZ> topPoints = new ArrayList<>(listXYZ(vectors,
                            baseEle + data.getBuildingPart().getLevelHeightAboveBase(level) + data.getBuildingPart().getLevelHeight(level)));

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

    }

}
