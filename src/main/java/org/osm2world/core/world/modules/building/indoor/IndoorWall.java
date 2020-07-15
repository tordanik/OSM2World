package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.map_data.data.*;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.modules.building.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.*;
import static org.osm2world.core.math.VectorXZ.listXYZ;
import static org.osm2world.core.util.ValueParseUtil.parseLevels;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.inheritTags;

public class IndoorWall implements Renderable {

    private final Float wallHeight;
    private final Float floorHeight;

    private List<MapNode> nodes;
    private List<SegmentNodes> wallSegmentNodes = new ArrayList<>();

    static List<SegmentLevelPair> allRenderedWallSegments = new ArrayList<>();

    private final IndoorObjectData data;

    //TODO account for height of wall
    public IndoorWall(IndoorObjectData objectData){

        this.data = objectData;

        this.wallHeight = data.getTopOfTopLevelHeightAboveBase().floatValue();
		this.floorHeight = data.getBuildingPart() == null ? 0 : (float) data.getLevelHeightAboveBase();

		nodes = ((MapWaySegment) data.getMapElement()).getWay().getNodes();

        splitIntoWalls();

    }

    public IndoorWall(BuildingPart buildingPart, MapElement element){

        data = new IndoorObjectData(buildingPart, element);
        this.floorHeight = (float) buildingPart.calculateFloorHeight();
        this.wallHeight = data.getTopOfTopLevelHeightAboveBase().floatValue();

        if (element instanceof MapArea) {
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
        List<MapNode> intermediateNodes = new ArrayList<>();

        for (int i = 1; i < nodes.size(); i++) {

            MapNode node = nodes.get(i);

            if (isCornerOrEnd(i)) {
                wallSegmentNodes.add(new SegmentNodes(intermediateNodes, new LineSegmentXZ(prevNode.getPos(), node.getPos())));
                prevNode = node;
                intermediateNodes = new ArrayList<>();
            } else {
                intermediateNodes.add(node);
            }

        }

    }

    private class SegmentNodes {

        private List<MapNode> nodes;
        private LineSegmentXZ segment;

        SegmentNodes(List<MapNode> allNodes, LineSegmentXZ segment){
            nodes = allNodes;
            this.segment = segment;
        }

        List<MapNode> getNodes() { return nodes; }

        LineSegmentXZ getSegment() { return segment; }

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

            for (SegmentNodes wallSegData : wallSegmentNodes) {

                SegmentLevelPair pair = new SegmentLevelPair(wallSegData.getSegment(), level);

                if (!allRenderedWallSegments.contains(pair)) {

                    allRenderedWallSegments.add(pair);

                    List<VectorXZ> vectors = wallSegData.getSegment().getVertexList();


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

                    /* add windows that aren't on vertices */

                    for (MapNode node : wallSegData.getNodes()) {

						Set<Integer> objectLevels = new HashSet<>();
						objectLevels.add(min(parseLevels(node.getTags().getValue("level"), singletonList(0))));
						objectLevels.addAll(parseLevels(node.getTags().getValue("repeat_on"), emptyList()));

						objectLevels = objectLevels.stream()
								.map(l -> data.getBuildingPart().levelConversion(l))
								.collect(Collectors.toSet());

						Double offset = wallSegData.segment.offsetOf(wallSegData.segment.closestPoint(node.getPos()));
						VectorXZ posFront = new VectorXZ(offset, 0);
						VectorXZ posback = new VectorXZ(wallSegData.segment.getLength() - offset, 0);

						if (objectLevels.contains(level)) {

							if (node.getTags().containsKey("window")
                                && !node.getTags().contains("window", "no")) {

                                TagSet windowTags = inheritTags(node.getTags(), data.getTags());
                                WindowParameters params = new WindowParameters(windowTags, data.getBuildingPart().getLevelHeight(level));

                                GeometryWindow windowFront = new GeometryWindow(new VectorXZ(offset, params.breast), params);
                                GeometryWindow windowBack = new GeometryWindow(new VectorXZ(wallSegData.segment.getLength() - offset, params.breast), params);

                                mainSurface.addElementIfSpaceFree(windowFront);
                                backSurface.addElementIfSpaceFree(windowBack);

                            } else if (node.getTags().containsKey("door")) {

								DoorParameters params = DoorParameters.fromTags(node.getTags(), data.getBuildingPart().getTags());

								mainSurface.addElementIfSpaceFree(new Door(posFront, params));
								backSurface.addElementIfSpaceFree(new Door(posback, params));

							}
                        }
                    }

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
