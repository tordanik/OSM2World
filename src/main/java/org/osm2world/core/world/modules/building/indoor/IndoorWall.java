package org.osm2world.core.world.modules.building.indoor;

import org.osm2world.core.map_data.data.*;
import org.osm2world.core.math.*;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.modules.building.*;
import org.osm2world.core.world.modules.building.roof.Roof;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.math3.util.MathUtils.TWO_PI;
import static org.osm2world.core.math.VectorXZ.listXYZ;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.material.TexCoordUtil.triangleTexCoordLists;
import static org.osm2world.core.util.ValueParseUtil.parseLevels;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.inheritTags;

public class IndoorWall implements Renderable {

	private final double straightnessTolerance = 0.05;
	private final double wallThickness = 0.1;

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

        // TODO tolerance may need tweaking, possibly based on length of segment??

        if (abs(dot - 1) < straightnessTolerance) {
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
                wallSegmentNodes.add(new SegmentNodes(intermediateNodes, new LineSegmentXZ(prevNode.getPos(), node.getPos()), prevNode, node));
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
        private MapNode startNode;
        private MapNode endNode;

        SegmentNodes(List<MapNode> allNodes, LineSegmentXZ segment, MapNode startNode, MapNode endNode){
            nodes = allNodes;
            this.segment = segment;
            this.startNode = startNode;
            this.endNode = endNode;
        }

        List<MapNode> getNodes() { return nodes; }

        LineSegmentXZ getSegment() { return segment; }

		public MapNode getStartNode() { return startNode; }

		public MapNode getEndNode() { return endNode; }

		public boolean containsMapSegment(LineSegmentXZ linSeg){

        	List<VectorXZ> nodePositions = nodes.stream().map(n -> n.getPos()).collect(toList());

        	if (linSeg.p1 == startNode.getPos() && nodePositions.contains(linSeg.p2)) {
        		return true;
			}

        	if (linSeg.p2 == endNode.getPos() && nodePositions.contains(linSeg.p1)) {
        		return true;
			}

        	if (nodePositions.contains(linSeg.p1) && nodePositions.contains(linSeg.p2)) {
        		return true;
			}

        	return false;

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

    private List<VectorXYZ> generateTopPoints(Target target, List<VectorXZ> ends, Double heightAboveZero){

        /* quick return if not in roof */

        if (heightAboveZero <= data.getBuildingPart().getHeightWithoutRoof() + data.getBuildingPart().getBuildingPartBaseEle() + 1e-4) {
            return listXYZ(ends, heightAboveZero);
        }

        // TODO possibly calculated every time

        Collection<LineSegmentXZ> innerSegments = data.getBuildingPart().getRoof().getInnerSegments();

        List<VectorXZ> intersections = new ArrayList<>();
        intersections.add(ends.get(0));

        for (LineSegmentXZ roofSegment : innerSegments) {
            if (roofSegment.intersects(ends.get(0), ends.get(1))) {
                intersections.add(roofSegment.getIntersection(ends.get(0), ends.get(1)));
            }
        }

		intersections.add(ends.get(1));

		intersections.sort((v1, v2) -> Double.compare(v1.subtract(ends.get(0)).length(), v2.subtract(ends.get(0)).length()));

        Roof roof = data.getBuildingPart().getRoof();

        double levelHeightInRoof = heightAboveZero - data.getBuildingPart().getHeightWithoutRoof() - data.getBuildingPart().getBuildingPartBaseEle();
        List<VectorXZ> levelIntersections = new ArrayList<>();

        for (int i = 0; i < intersections.size() - 1; i++) {
        	if ((roof.getRoofHeightAt(intersections.get(i)) > levelHeightInRoof
					&& roof.getRoofHeightAt(intersections.get(i + 1)) < levelHeightInRoof)
					|| (roof.getRoofHeightAt(intersections.get(i + 1)) > levelHeightInRoof
					&& roof.getRoofHeightAt(intersections.get(i)) < levelHeightInRoof) ) {

        		double z1 = 0;
        		double z2 = roof.getRoofHeightAt(intersections.get(i + 1)) - roof.getRoofHeightAt(intersections.get(i));

        		double x1 = 0;
        		double x2 = intersections.get(i).distanceTo(intersections.get(i + 1));

        		LineSegmentXZ wallSegment = new LineSegmentXZ(new VectorXZ(x1, z1), new VectorXZ(x2, z2));

        		LineSegmentXZ levelSegment = new LineSegmentXZ(
        				new VectorXZ(x1, levelHeightInRoof - roof.getRoofHeightAt(intersections.get(i))),
						new VectorXZ(x2, levelHeightInRoof - roof.getRoofHeightAt(intersections.get(i))));

        		VectorXZ wallLevelInt =  wallSegment.getIntersection(levelSegment.p1, levelSegment.p2);

        		if (wallLevelInt != null) {

					VectorXZ inter = intersections.get(i).add(
							intersections.get(i + 1).subtract(intersections.get(i))
									.normalize().mult(wallLevelInt.getX()));

					levelIntersections.add(inter);

				}
			}
		}

        intersections.addAll(levelIntersections);

        intersections.sort((v1, v2) -> Double.compare(v1.subtract(ends.get(0)).length(), v2.subtract(ends.get(0)).length()));

		List<VectorXYZ> limitedHeights = new ArrayList<>();

        for (VectorXZ intersection : intersections) {
            limitedHeights.add(
            		intersection.xyz(Math.min(data.getBuildingPart().getBuildingPartBaseEle()
							+ data.getBuildingPart().getHeightWithoutRoof()
							+ data.getBuildingPart().getRoof().getRoofHeightAt(intersection),
							heightAboveZero)));
        }

        return limitedHeights;

    }

	private class TagLineSegPair{

    	private final TagSet tags;
    	private final LineSegmentXZ lineSeg;

    	TagLineSegPair(TagSet tags, LineSegmentXZ lineSeg){
    		this.tags = tags;
    		this.lineSeg = lineSeg;
		}

		TagSet getTags() { return tags; }

		LineSegmentXZ getLineSegment() { return lineSeg; }

	}

    private List<VectorXZ> getNewEndPoint(SegmentNodes wallSegData, boolean end, int level){

		MapNode endNode;
		LineSegmentXZ wallSegSegment;
		List<TagLineSegPair> allPairs = new ArrayList<>();

		if (end) {
			endNode = wallSegData.getEndNode();
			wallSegSegment = wallSegData.getSegment();
		} else {
    		endNode = wallSegData.getStartNode();
    		wallSegSegment = wallSegData.getSegment().reverse();
		}


		/* collect segments of connected areas */

		List<MapArea> areas = new ArrayList<>(endNode.getAdjacentAreas());

		for (int i = 0; i < areas.size(); i ++) {
			List<MapAreaSegment> allSegments = new ArrayList<>(areas.get(i).getAreaSegments())
					.stream()
					.filter(v -> v.getEndNode() == endNode || v.getStartNode() == endNode)
					.collect(toList());

			for (MapAreaSegment s : allSegments) {
				allPairs.add(new TagLineSegPair(areas.get(i).getTags(), s.getLineSegment()));
			}
		}


		/* collect segments of connected ways */

		List<MapWay> ways = new ArrayList<>(endNode.getConnectedWays());

		for (int i = 0; i < ways.size(); i++) {
			List<MapWaySegment> allSegments = new ArrayList<>(ways.get(i).getWaySegments())
					.stream()
					.filter(v -> v.getEndNode() == endNode || v.getStartNode() == endNode)
					.collect(toList());

			for (MapWaySegment s : allSegments) {
				allPairs.add(new TagLineSegPair(ways.get(i).getTags(), s.getLineSegment()));
			}
		}


		/* find closest wall clockwise and anticlockwise */

		double maxAngle = 0;
		LineSegmentXZ maxLineSegment = null;

		double minAngle = 7;
		LineSegmentXZ minLineSegment = null;

		for (TagLineSegPair segmentPair : allPairs) {
			LineSegmentXZ segment = segmentPair.getLineSegment();

			if ((segmentPair.getTags().contains("indoor", "wall") || segmentPair.getTags().contains("indoor", "room"))
					&& parseLevels(segmentPair.getTags().getValue("level")).stream().map(l -> data.getBuildingPart().levelConversion(l)).collect(toList()).contains(level)
					&& !segment.equals(wallSegSegment)
					&& !segment.equals(wallSegSegment.reverse())
					&& !wallSegData.containsMapSegment(segment)
					&& abs(abs(segment.getDirection().normalize().dot(wallSegSegment.getDirection().normalize())) - 1) >= straightnessTolerance) {

				LineSegmentXZ segmentToCheck;

				// make sure all connected segments are directed into segment end nodes

				if (segment.p2 != wallSegSegment.p2) {
					segmentToCheck = segment.reverse();
				} else {
					segmentToCheck = segment;
				}

				double clockwiseAngleBetween = segmentToCheck.getDirection().angle() - wallSegSegment.getDirection().angle();

				clockwiseAngleBetween = clockwiseAngleBetween <= 0 ? TWO_PI + clockwiseAngleBetween : clockwiseAngleBetween;

				if (maxAngle < clockwiseAngleBetween) {
					maxAngle = clockwiseAngleBetween;
					maxLineSegment = segmentToCheck;
				}
				if (clockwiseAngleBetween < minAngle) {
					minAngle = clockwiseAngleBetween;
					minLineSegment = segmentToCheck;
				}

			}
		}


		/* find intersections */

		List<VectorXZ> result = new ArrayList<>();

		List<VectorXZ> rightOffset = wallSegSegment.getVertexList().stream()
				.map(v -> v.add(wallSegSegment.getDirection().rightNormal().mult(wallThickness)))
				.collect(toList());
		LineSegmentXZ offsetSegRight = new LineSegmentXZ(rightOffset.get(0), rightOffset.get(1));

		List<VectorXZ> leftOffset = wallSegSegment.getVertexList().stream()
				.map(v -> v.add(wallSegSegment.getDirection().rightNormal().mult(-wallThickness)))
				.collect(toList());
		LineSegmentXZ offsetSegLeft = new LineSegmentXZ(leftOffset.get(0), leftOffset.get(1));

		VectorXZ tempResult = offsetSegRight.getVertexList().get(1);

		if (maxLineSegment != null) {
			VectorXZ intersection = GeometryUtil.getLineIntersection(offsetSegRight.p1,
					offsetSegRight.getDirection(),
					maxLineSegment.p2.add(maxLineSegment.getDirection().rightNormal().mult(-wallThickness)),
					maxLineSegment.getDirection());
			if (intersection != null) {
				tempResult = intersection;
			}
		}

		result.add(tempResult);

		tempResult = offsetSegLeft.getVertexList().get(1);

		if (minLineSegment != null) {
			VectorXZ intersection = GeometryUtil.getLineIntersection(offsetSegLeft.p1,
					offsetSegLeft.getDirection(),
					minLineSegment.p2.add(minLineSegment.getDirection().rightNormal().mult(wallThickness)),
					minLineSegment.getDirection());
			if (intersection != null) {
				tempResult = intersection;
			}
		}

		result.add(tempResult);

    	return result;

	}

    private List<VectorXZ> getNewEndPoints(SegmentNodes wallSegData, int level){

    	List<VectorXZ> result = new ArrayList<>();

		result.addAll(getNewEndPoint(wallSegData, false, level));
		result.addAll(getNewEndPoint(wallSegData, true, level));

		return result;

	}

    @Override
    public void renderTo(Target target) {

        double baseEle = data.getBuildingPart().getBuildingPartBaseEle();

        Material material = BuildingPart.buildMaterial(data.getTags().getValue("material"), null, Materials.BRICK, false);

        for (Integer level : data.getRenderableLevels()) {

        	double ceilingHeight = baseEle + data.getBuildingPart().getLevelHeightAboveBase(level) + data.getBuildingPart().getLevelHeight(level);

            for (SegmentNodes wallSegData : wallSegmentNodes) {

                SegmentLevelPair pair = new SegmentLevelPair(wallSegData.getSegment(), level);

                if (!allRenderedWallSegments.contains(pair)) {

                    allRenderedWallSegments.add(pair);

					List<VectorXZ> endPoints = getNewEndPoints(wallSegData, level);

					/* front wall surface */

					List<VectorXZ> bottomPointsXZ = new ArrayList<>();
					bottomPointsXZ.add(endPoints.get(3));
					bottomPointsXZ.add(endPoints.get(0));

					List<VectorXYZ> bottomPoints = new ArrayList<>(listXYZ(bottomPointsXZ,
							baseEle + data.getBuildingPart().getLevelHeightAboveBase(level)));

                    List<VectorXYZ> topPoints = generateTopPoints(target , bottomPointsXZ, ceilingHeight);

                    // TODO check if outside roof before generateTopPoints

                    if (topPoints.get(0).y < bottomPoints.get(0).y || topPoints.get(topPoints.size() - 1).y < bottomPoints.get(1).y) {
                        topPoints =  new ArrayList<>(listXYZ(bottomPointsXZ, ceilingHeight));
                    }

                    WallSurface mainSurface = new WallSurface(material, bottomPoints, topPoints);

                    /* back wall surface */

                    List<VectorXZ> backBottomPointsXZ = new ArrayList<>();
                    backBottomPointsXZ.add(endPoints.get(2));
                    backBottomPointsXZ.add(endPoints.get(1));

					Collections.reverse(backBottomPointsXZ);

					List<VectorXYZ> backBottomPoints = new ArrayList<>(listXYZ(backBottomPointsXZ,
							baseEle + data.getBuildingPart().getLevelHeightAboveBase(level)));

                    List<VectorXYZ> backTopPoints = generateTopPoints(target , backBottomPointsXZ, ceilingHeight);

					if (backTopPoints.get(0).y < backBottomPoints.get(0).y || backTopPoints.get(backTopPoints.size() - 1).y < backBottomPoints.get(1).y) {
						backTopPoints =  new ArrayList<>(listXYZ(backBottomPointsXZ, ceilingHeight));
					}

                    WallSurface backSurface = new WallSurface(material, backBottomPoints, backTopPoints);


					/* generate wall edges */

					WallSurface rightSurface = null;
					WallSurface leftSurface = null;

					// TODO avoid needing a try

					try {

						List<VectorXZ> bottomVertexLoop = new ArrayList<>(endPoints);
						bottomVertexLoop.add(endPoints.get(0));

						SimplePolygonXZ bottomPolygonXZ = new SimplePolygonXZ(bottomVertexLoop);
						Collection<TriangleXYZ> bottomTriangles = TriangulationUtil.
								triangulate(bottomPolygonXZ.asPolygonWithHolesXZ())
								.stream()
								.map(t -> t.makeClockwise().xyz(baseEle + data.getBuildingPart().getLevelHeightAboveBase(level)))
								.collect(toList());

						Collection<TriangleXYZ> tempTopTriangles = TriangulationUtil.
								triangulate(bottomPolygonXZ.asPolygonWithHolesXZ())
								.stream()
								.map(t -> t.makeCounterclockwise().xyz(ceilingHeight))
								.collect(toList());

						target.drawTriangles(material, bottomTriangles, triangleTexCoordLists(bottomTriangles, material, GLOBAL_X_Z));
						target.drawTriangles(material, tempTopTriangles, triangleTexCoordLists(tempTopTriangles, material, GLOBAL_X_Z));


						rightSurface = new WallSurface(material,
								Arrays.asList(bottomPoints.get(1), backBottomPoints.get(0)),
								Arrays.asList(topPoints.get(0), backTopPoints.get(backBottomPoints.size() - 1)));

						leftSurface = new WallSurface(material,
								Arrays.asList(backBottomPoints.get(1), bottomPoints.get(0)),
								Arrays.asList(backTopPoints.get(0), topPoints.get(topPoints.size() - 1)));

					} catch (InvalidGeometryException e) {}

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

                    if (mainSurface != null && backSurface != null) {
                        mainSurface.renderTo(target, new VectorXZ(0, -floorHeight), false, 0);
                        backSurface.renderTo(target, new VectorXZ(0, -floorHeight), false, 0);
                        if (leftSurface != null && rightSurface != null) {
							rightSurface.renderTo(target, new VectorXZ(0, -floorHeight), false, 0);
							leftSurface.renderTo(target, new VectorXZ(0, -floorHeight), false, 0);
						}
                    }
                }

            }
        }

    }

}
