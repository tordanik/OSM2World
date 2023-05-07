package org.osm2world.core.world.modules.building.indoor;

import static java.lang.Math.abs;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.math3.util.MathUtils.TWO_PI;
import static org.osm2world.core.math.VectorXZ.listXYZ;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.triangleTexCoordLists;
import static org.osm2world.core.util.ValueParseUtil.parseLevels;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.inheritTags;

import java.util.*;

import org.osm2world.core.conversion.ConversionLog;
import org.osm2world.core.map_data.data.*;
import org.osm2world.core.math.*;
import org.osm2world.core.math.algorithms.TriangulationUtil;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.attachment.AttachmentSurface;
import org.osm2world.core.world.modules.building.Building.NodeLevelPair;
import org.osm2world.core.world.modules.building.*;
import org.osm2world.core.world.modules.building.roof.Roof;

import com.google.common.collect.Sets;

public class IndoorWall implements Renderable {

	private final double straightnessTolerance = 0.001;
	private final double wallThickness = 0.1;
	private final double topOffset = 0.001;

    private final Float wallHeight;
    private final Float floorHeight;

    private List<MapNode> nodes;
    private List<SegmentNodes> wallSegmentNodes = new ArrayList<>();

    static List<SegmentLevelPair> allRenderedWallSegments = new ArrayList<>();

    private final IndoorObjectData data;

    private List<AttachmentSurface> attachmentSurfacesList;

    private static final Material defaultInnerMaterial = Materials.CONCRETE;

    //TODO account for height of wall
    public IndoorWall(IndoorObjectData objectData){

        this.data = objectData;

        this.wallHeight = data.getTopOfTopLevelHeightAboveBase().floatValue();
		this.floorHeight = data.getBuildingPart() == null ? 0 : (float) data.getLevelHeightAboveBase();

		if (data.getMapElement() instanceof MapWaySegment) {
			nodes = waySegmentNodes((MapWaySegment) data.getMapElement());
		} else if (data.getMapElement() instanceof MapArea) {
			// for tagging area=yes, indoor=wall
			nodes = areaNodes((MapArea) data.getMapElement());
		} else {
			nodes = new ArrayList<>();
		}

        splitIntoWalls();

    }

    public IndoorWall(BuildingPart buildingPart, MapElement element){

        data = new IndoorObjectData(buildingPart, element);
        this.floorHeight = (float) buildingPart.levelStructure.bottomHeight();
        this.wallHeight = data.getTopOfTopLevelHeightAboveBase().floatValue();

        if (element instanceof MapArea) {
			nodes = areaNodes((MapArea) element);
		} else {
        	nodes = new ArrayList<>();
		}

        splitIntoWalls();

    }

    private List<MapNode> areaNodes(MapArea area){
    	return area.getBoundaryNodes();
	}

	private List<MapNode> waySegmentNodes(MapWaySegment segment){
		return segment.getWay().getNodes();
	}

	public List<SegmentNodes> getWallSegmentNodes() { return wallSegmentNodes; }

	public double getWallThickness() { return wallThickness; }

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

    	if (!nodes.isEmpty()) {

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
    }

	public Collection<AttachmentSurface> getAttachmentSurfaces() {

		if (attachmentSurfacesList == null) {
			attachmentSurfacesList = new ArrayList<>();
			this.renderTo(null, false, true);
		}

		return attachmentSurfacesList;
	}

	public class SegmentNodes {

        private List<MapNode> nodes;
        private LineSegmentXZ segment;
        private MapNode startNode;
        private MapNode endNode;
		List<VectorXZ> nodePositions;

        SegmentNodes(List<MapNode> intermediateNodes, LineSegmentXZ segment, MapNode startNode, MapNode endNode){
            nodes = intermediateNodes;
            this.segment = segment;
            this.startNode = startNode;
            this.endNode = endNode;
			this.nodePositions = nodes.stream().map(MapNode::getPos).collect(toList());
        }

        List<MapNode> getNodes() { return nodes; }

        LineSegmentXZ getSegment() { return segment; }

		public MapNode getStartNode() { return startNode; }

		public MapNode getEndNode() { return endNode; }

		public boolean containsMapSegment(LineSegmentXZ linSeg){

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
        MapNode startNode;
        MapNode endNode;

        SegmentLevelPair(LineSegmentXZ segment, Integer level, MapNode startNode, MapNode endNode){
            this.segment = segment;
            this.level = level;
            this.startNode = startNode;
            this.endNode = endNode;
        }

        private Boolean roughlyEquals(LineSegmentXZ seg){
            return (seg.p1.subtract(this.segment.p1).lengthSquared() + seg.p2.subtract(this.segment.p2).lengthSquared() < 0.0001)
                    || (seg.p2.subtract(this.segment.p1).lengthSquared() + seg.p1.subtract(this.segment.p2).lengthSquared() < 0.0001);
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
            if ((this.startNode.equals(temp.startNode) && this.endNode.equals(temp.endNode))
					|| (this.endNode.equals(temp.startNode) && this.startNode.equals(temp.endNode))) {
            	if (this.level.equals(temp.level)) {
					return true;
				}
			}
            return roughlyEquals(temp.segment) && this.level.equals(temp.level);
        }


    }

    private List<VectorXYZ> generateTopPoints(List<VectorXYZ> bottomPoints, Double heightAboveZero){

		Roof roof = data.getBuildingPart().getRoof();
		double heightWithoutRoof = data.getBuildingPart().levelStructure.heightWithoutRoof();

		if (roof.getRoofHeightAt(bottomPoints.get(0).xz()) + heightWithoutRoof  < bottomPoints.get(0).y || roof.getRoofHeightAt(bottomPoints.get(1).xz()) + heightWithoutRoof < bottomPoints.get(1).y) {
			return new ArrayList<>(listXYZ(bottomPoints.stream().map(p -> p.xz()).collect(toList()), heightAboveZero));
		}

		List<VectorXZ> ends = bottomPoints.stream().map(p -> p.xz()).collect(toList());

		/* quick return if not in roof */

		if (heightAboveZero <= data.getBuildingPart().levelStructure.heightWithoutRoof()
				+ data.getBuildingPart().getBuilding().getGroundLevelEle() + 1e-4) {
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

        double levelHeightInRoof = heightAboveZero - data.getBuildingPart().levelStructure.heightWithoutRoof()
        		- data.getBuildingPart().getBuilding().getGroundLevelEle();
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
            		intersection.xyz(Math.min(data.getBuildingPart().getBuilding().getGroundLevelEle()
							+ data.getBuildingPart().levelStructure.heightWithoutRoof()
							+ data.getBuildingPart().getRoof().getRoofHeightAt(intersection),
							heightAboveZero)));
        }

        return limitedHeights;

    }

	private class TagLineSegPair{

		// may contain duplicates
		private List<Tag> tags;
		private List<Tag> dedupeTags;
		private final LineSegmentXZ lineSeg;
		private final MapNode startNode;
		private final MapNode endNode;

		TagLineSegPair(List<Tag> tags, LineSegmentXZ lineSeg, MapNode startNode, MapNode endNode){
			this.tags = new ArrayList<>(tags);
			this.dedupeTags = new ArrayList<>(tags);
			this.lineSeg = lineSeg;
			this.startNode = startNode;
			this.endNode = endNode;
		}

		// May not be fully representative of tags list as tags with duplicated keys are removed
		TagSet getTagSet() {
				return TagSet.of(Sets.newHashSet(dedupeTags));
		}

		void addTags(List<Tag> newTags) {

			tags.addAll(newTags);

			List<Tag> temp = new ArrayList<>();

			for (Tag t : newTags) {

				boolean uniqueKey = true;

				for (Tag oldTag : dedupeTags) {
					if (t.key.equals(oldTag.key)) {
						uniqueKey = false;
					}
				}

				if (uniqueKey) {
					temp.add(t);
				}
			}

			dedupeTags.addAll(temp);

		}

		LineSegmentXZ getLineSegment() { return lineSeg; }

		List<Tag> getTags() { return tags; }

		public MapNode getStartNode() { return startNode; }

		public MapNode getEndNode() { return endNode; }

	}

	private VectorXZ NormalIfOnBuildingEdge(SegmentNodes lineSegmentNodes){

//		List<VectorXZ> buildingVertices = data.getBuildingPart().getBuilding().getOutlinePolygonXZ().getVertexList();

		LineSegmentXZ line = lineSegmentNodes.getSegment();

		SimplePolygonXZ outline = data.getBuildingPart().getPolygon().getOuter();
		VectorXZ rightNorm = line.getDirection().rightNormal().mult(wallThickness + 0.01);

		SimplePolygonXZ roughWallOutline = new SimplePolygonXZ(asList(line.p2.add(rightNorm),
				line.p2.subtract(rightNorm),
				line.p1.subtract(rightNorm),
				line.p1.add(rightNorm),
				line.p2.add(rightNorm)));

		if (outline.contains(roughWallOutline)) {
			List<SimplePolygonXZ> holes = data.getBuildingPart().getPolygon().getHoles();

			for (SimplePolygonXZ hole : holes) {
				if (hole.intersects(roughWallOutline)) {
					if (!hole.isClockwise()) {
						return hole.getClosestSegment(line.getCenter()).getDirection().rightNormal().mult(wallThickness + 0.01);
					} else {
						return hole.getClosestSegment(line.getCenter()).getDirection().rightNormal().mult(-1 * (wallThickness + 0.01));
					}
				}
			}

			return null;
		} else {
			if (outline.isClockwise()) {
				return outline.getClosestSegment(line.getCenter()).getDirection().rightNormal().mult(wallThickness + 0.01);
			} else {
				return outline.getClosestSegment(line.getCenter()).getDirection().rightNormal().mult(-1 * (wallThickness + 0.01));
			}
		}

//		for (int i = 0; i < buildingVertices.size() - 1; i++) {
//			if ((line.p1.equals(buildingVertices.get(i)) && line.p2.equals(buildingVertices.get(i+1)))
//					|| lineSegmentNodes.containsMapSegment(new LineSegmentXZ(buildingVertices.get(i), buildingVertices.get(i + 1)))) {
//
//				rightNorm = new LineSegmentXZ(buildingVertices.get(i), buildingVertices.get(i+1)).getDirection().rightNormal();
//
//				if (data.getBuildingPart().getBuilding().getOutlinePolygonXZ().isClockwise()) {
//					return rightNorm.mult(wallThickness + 0.01);
//				}
//
//				return rightNorm.mult(-1 * (wallThickness + 0.01));
//
//			} else if ((line.p2.equals(buildingVertices.get(i)) && line.p1.equals(buildingVertices.get(i + 1)))
//					|| lineSegmentNodes.containsMapSegment(new LineSegmentXZ(buildingVertices.get(i + 1), buildingVertices.get(i)))) {
//
//				rightNorm = new LineSegmentXZ(buildingVertices.get(i+1), buildingVertices.get(i)).getDirection().rightNormal();
//
//				if (data.getBuildingPart().getBuilding().getOutlinePolygonXZ().isClockwise()) {
//					return rightNorm.mult(-1 * (wallThickness + 0.01));
//				}
//
//				return rightNorm.mult(wallThickness + 0.01);
//
//			}
//		}
//
//		return null;

	}

	private LineSegmentXZ getBuildingEdgeLineSeg(LineSegmentXZ line, VectorXZ rightNorm) {
    	return new LineSegmentXZ(line.p1.add(rightNorm), line.p2.add(rightNorm));
	}

    private List<VectorXZ> getNewEndPoint(SegmentNodes wallSegData, boolean end, int level, double heightAboveGround, double ceilingHeightAboveGround){

		MapNode startNode;
		MapNode endNode;
		LineSegmentXZ wallSegSegment;
		List<TagLineSegPair> allTagLinePairs = new ArrayList<>();

		if (end) {
			startNode = wallSegData.getStartNode();
			endNode = wallSegData.getEndNode();
			wallSegSegment = wallSegData.getSegment();
		} else {
    		startNode = wallSegData.getEndNode();
			endNode = wallSegData.getStartNode();
    		wallSegSegment = wallSegData.getSegment().reverse();
		}


		/* collect connected segments of connected areas */

		List<MapArea> areas = new ArrayList<>(endNode.getAdjacentAreas());

		for (int i = 0; i < areas.size(); i ++) {
			List<MapAreaSegment> allSegments = new ArrayList<>(areas.get(i).getAreaSegments())
					.stream()
					.filter(v -> v.getEndNode() == endNode || v.getStartNode() == endNode)
					.collect(toList());

			for (MapAreaSegment s : allSegments) {
				allTagLinePairs.add(new TagLineSegPair(areas.get(i).getTags().stream().collect(toList()), s.getLineSegment(), s.getStartNode(), s.getEndNode()));
			}
		}


		/* collect connected segments of connected ways */

		List<MapWay> ways = new ArrayList<>(endNode.getConnectedWays());

		for (int i = 0; i < ways.size(); i++) {
			List<MapWaySegment> allSegments = new ArrayList<>(ways.get(i).getWaySegments())
					.stream()
					.filter(v -> v.getEndNode() == endNode || v.getStartNode() == endNode)
					.collect(toList());

			for (MapWaySegment s : allSegments) {
				allTagLinePairs.add(new TagLineSegPair(ways.get(i).getTags().stream().collect(toList()), s.getLineSegment(), s.getStartNode(), s.getEndNode()));
			}
		}


		/* deduplicate and filter */

		List<TagLineSegPair> tempPairs = new ArrayList<>();

		for (TagLineSegPair pairData : allTagLinePairs) {

			if ((pairData.getTagSet().contains("indoor", "wall") || pairData.getTagSet().contains("indoor", "room")) && pairData.getTagSet().containsKey("level")) {

				List<Integer> parsedLevels = parseLevels(pairData.getTagSet().getValue("level"));

				if (parsedLevels != null) {

					if (parsedLevels.contains(level)) {

						boolean duplicate = false;

						for (int i = 0; i < tempPairs.size(); i++) {

							TagLineSegPair p = tempPairs.get(i);

							if ((p.getLineSegment().equals(pairData.getLineSegment()) || p.getLineSegment().equals(pairData.getLineSegment().reverse()))) {

								duplicate = true;

								TagLineSegPair l = tempPairs.remove(i);
								l.addTags(pairData.getTags());
								tempPairs.add(l);

								break;
							}
						}

						if (!duplicate) {
							tempPairs.add(pairData);
						}
					}
				}
			}
		}

		allTagLinePairs = tempPairs;


		/* find closest wall clockwise and anticlockwise */

		double maxAngle = 0;
		TagLineSegPair maxLineSegment = null;

		double minAngle = 7;
		TagLineSegPair minLineSegment = null;

		for (TagLineSegPair segmentPair : allTagLinePairs) {

			LineSegmentXZ segment = segmentPair.getLineSegment();
			TagSet pairSet = segmentPair.getTagSet();

			LineSegmentXZ segmentToCheck;

			// make sure all connected segments are directed into endNode

			if (segment.p2 != wallSegSegment.p2) {
				segmentToCheck = segment.reverse();
			} else {
				segmentToCheck = segment;
			}

			if ((pairSet.contains("indoor", "wall") || pairSet.contains("indoor", "room"))
					&& !segmentToCheck.equals(wallSegSegment)
					&& !wallSegData.containsMapSegment(segmentToCheck)
					&& !wallSegData.containsMapSegment(segmentToCheck.reverse()) ){
//					&& abs(abs(segmentToCheck.getDirection().normalize().dot(wallSegSegment.getDirection().normalize())) - 1) >= straightnessTolerance) {

				double clockwiseAngleBetween = segmentToCheck.getDirection().angle() - wallSegSegment.getDirection().angle();
				clockwiseAngleBetween = clockwiseAngleBetween <= 0 ? TWO_PI + clockwiseAngleBetween : clockwiseAngleBetween;

				if (maxAngle < clockwiseAngleBetween) {
					maxAngle = clockwiseAngleBetween;
					maxLineSegment = new TagLineSegPair(segmentPair.getTags(), segmentToCheck, segmentPair.getStartNode(), segmentPair.getEndNode());
				}
				if (clockwiseAngleBetween < minAngle) {
					minAngle = clockwiseAngleBetween;
					minLineSegment = new TagLineSegPair(segmentPair.getTags(), segmentToCheck, segmentPair.getStartNode(), segmentPair.getEndNode());
				}
			}
		}


		/* move this wall segment if on outer edge of building */

		final VectorXZ thisBuildingEdgeRightNorm = NormalIfOnBuildingEdge(new SegmentNodes(wallSegData.getNodes(), wallSegSegment, startNode, endNode) );

		if (thisBuildingEdgeRightNorm != null) {
			wallSegSegment = new LineSegmentXZ(wallSegSegment.p1.add(thisBuildingEdgeRightNorm), wallSegSegment.p2.add(thisBuildingEdgeRightNorm));
		}


		/* find intersections */

		List<VectorXZ> result = new ArrayList<>();

		// lambda needs final
		final LineSegmentXZ finalSegSegment = new LineSegmentXZ(wallSegSegment.p1, wallSegSegment.p2);

		List<VectorXZ> rightOffset = wallSegSegment.vertices().stream()
				.map(v -> v.add(finalSegSegment.getDirection().rightNormal().mult(wallThickness)))
				.collect(toList());
		LineSegmentXZ offsetSegRight = new LineSegmentXZ(rightOffset.get(0), rightOffset.get(1));

		List<VectorXZ> leftOffset = wallSegSegment.vertices().stream()
				.map(v -> v.add(finalSegSegment.getDirection().rightNormal().mult(-wallThickness)))
				.collect(toList());
		LineSegmentXZ offsetSegLeft = new LineSegmentXZ(leftOffset.get(0), leftOffset.get(1));


		VectorXZ tempResult = offsetSegRight.vertices().get(1);

		if (maxLineSegment != null
				&& abs(abs(maxLineSegment.getLineSegment().getDirection().normalize().dot(wallSegSegment.getDirection().normalize())) - 1) >= straightnessTolerance) {

			VectorXZ maxOnBuildingEdgeRightNorm = NormalIfOnBuildingEdge(
					new SegmentNodes(asList(maxLineSegment.getStartNode(), maxLineSegment.getEndNode()),
							maxLineSegment.getLineSegment(), maxLineSegment.getStartNode(), maxLineSegment.getEndNode()));

			if (maxOnBuildingEdgeRightNorm != null) {
				maxLineSegment = new TagLineSegPair(maxLineSegment.getTags(),
						getBuildingEdgeLineSeg(maxLineSegment.getLineSegment(), maxOnBuildingEdgeRightNorm),
						maxLineSegment.getStartNode(), maxLineSegment.getEndNode());
			}

			VectorXZ intersection = GeometryUtil.getLineIntersection(offsetSegRight.p1,
					offsetSegRight.getDirection(),
					maxLineSegment.getLineSegment().p2.add(maxLineSegment.getLineSegment().getDirection().rightNormal().mult(-wallThickness)),
					maxLineSegment.getLineSegment().getDirection());

			if (intersection != null) {
				tempResult = intersection;
			}
		}

		result.add(tempResult);


		tempResult = offsetSegLeft.vertices().get(1);

		if (minLineSegment != null
				&& abs(abs(minLineSegment.getLineSegment().getDirection().normalize().dot(wallSegSegment.getDirection().normalize())) - 1) >= straightnessTolerance) {

			VectorXZ minOnBuildingEdgeRightNorm = NormalIfOnBuildingEdge(
					new SegmentNodes(asList(minLineSegment.getStartNode(), minLineSegment.getEndNode()),
							minLineSegment.getLineSegment(), minLineSegment.getStartNode(), minLineSegment.getEndNode()));

			if (minOnBuildingEdgeRightNorm != null) {
				minLineSegment = new TagLineSegPair(minLineSegment.getTags(),
						getBuildingEdgeLineSeg(minLineSegment.getLineSegment(), minOnBuildingEdgeRightNorm),
						minLineSegment.getStartNode(), minLineSegment.getEndNode());
			}

			VectorXZ intersection = GeometryUtil.getLineIntersection(offsetSegLeft.p1,
					offsetSegLeft.getDirection(),
					minLineSegment.getLineSegment().p2.add(minLineSegment.getLineSegment().getDirection().rightNormal().mult(wallThickness)),
					minLineSegment.getLineSegment().getDirection());

			if (intersection != null) {
				tempResult = intersection;
			}
		}

		result.add(tempResult);

		data.getBuildingPart().getBuilding().addLineSegmentToPolygonMap(endNode, level, new LineSegmentXZ(result.get(0), result.get(1)), heightAboveGround, ceilingHeightAboveGround);

		return result;

	}


    public List<VectorXZ> getNewEndPoints(SegmentNodes wallSegData, int level, double heightAboveGround, double ceilingHeightAboveGround){

    	List<VectorXZ> result = new ArrayList<>();

		result.addAll(getNewEndPoint(wallSegData, false, level, heightAboveGround, ceilingHeightAboveGround));
		result.addAll(getNewEndPoint(wallSegData, true, level, heightAboveGround, ceilingHeightAboveGround));

		return result;

	}

	private static boolean roughlyEqual(LineSegmentXZ l1, LineSegmentXZ l2){
		return (roughlyEqual(l1.p1, l2.p1) && roughlyEqual(l1.p2, l2.p2))
				|| (roughlyEqual(l1.p1, l2.p2) && roughlyEqual(l1.p2, l2.p1));
	}

	private static boolean roughlyEqual(VectorXZ v1, VectorXZ v2){
    	return v1.subtract(v2).lengthSquared() < 0.00001;
	}

	public static void renderNodePolygons(Target target, Map<NodeLevelPair, List<LineSegmentXZ>> nodeToLineSegments){
		for (Map.Entry<NodeLevelPair, List<LineSegmentXZ>> entry : nodeToLineSegments.entrySet()) {

			NodeLevelPair nodeAndLevel = entry.getKey();
			List<LineSegmentXZ> lineSegments = entry.getValue();

			/* deduplicate line segments */

			List<LineSegmentXZ> dedupeLineSegments = new ArrayList<>();

			dedupeLineSegments.add(lineSegments.get(0));

			for (LineSegmentXZ line : lineSegments) {
				if (dedupeLineSegments.stream().noneMatch(l -> roughlyEqual(l, line))) {
					dedupeLineSegments.add(line);
				}
			}

			/* create polygon */

			List<VectorXZ> vertices = new ArrayList<>();

			LineSegmentXZ initialSeg = dedupeLineSegments.get(0);
			vertices.add(initialSeg.p1);
			vertices.add(initialSeg.p2);

			dedupeLineSegments.remove(0);

			int n = 0;
			int dedupeListSize = dedupeLineSegments.size();

			while (dedupeLineSegments.size() > 0 && !vertices.get(0).equals(vertices.get(vertices.size() - 1)) && n < dedupeListSize) {
				for (LineSegmentXZ segment : dedupeLineSegments) {
					if (roughlyEqual(segment.p1, vertices.get(vertices.size() - 1))) {
						if (roughlyEqual(segment.p2, vertices.get(0))) {
							vertices.add(vertices.get(0));
						} else {
							vertices.add(segment.p2);
						}
						dedupeLineSegments.remove(segment);
						break;
					} else if (roughlyEqual(segment.p2, vertices.get(vertices.size() - 1))) {
						if (roughlyEqual(segment.p1, vertices.get(0))) {
							vertices.add(vertices.get(0));
						} else {
							vertices.add(segment.p1);
						}
						dedupeLineSegments.remove(segment);
						break;
					}
				}
				n++;
			}

			if (!vertices.get(0).equals(vertices.get(vertices.size() - 1))) {
				vertices.add(vertices.get(0));
			}

			if (vertices.size() > 3) {

				/* render polygon */

				try {

					SimplePolygonXZ polygon = new SimplePolygonXZ(vertices).makeCounterclockwise();

					Collection<TriangleXZ> triangles = TriangulationUtil.triangulate(polygon, emptyList());

					List<TriangleXYZ> trianglesXYZBottom = triangles.stream()
							.map(t -> t.makeClockwise().xyz(nodeAndLevel.getHeightAboveGround()))
							.collect(toList());

					List<TriangleXYZ> trianglesXYZTop = triangles.stream()
							.map(t -> t.makeCounterclockwise().xyz(nodeAndLevel.getCeilingHeightAboveGround() - 0.0001))
							.collect(toList());

					VectorXYZ bottom = new VectorXYZ(0, nodeAndLevel.getHeightAboveGround(),0);
					VectorXYZ top = new VectorXYZ(0, nodeAndLevel.getCeilingHeightAboveGround() - 0.0001,0);

					List<VectorXYZ> path = new ArrayList<>();
					path.add(bottom);
					path.add(top);

					target.drawExtrudedShape(defaultInnerMaterial, polygon, path, null, null, null, null);

					target.drawTriangles(defaultInnerMaterial, trianglesXYZBottom,
							triangleTexCoordLists(trianglesXYZBottom, Materials.BRICK, GLOBAL_X_Z));

					target.drawTriangles(defaultInnerMaterial, trianglesXYZTop,
							triangleTexCoordLists(trianglesXYZTop, Materials.BRICK, GLOBAL_X_Z));


				} catch (InvalidGeometryException e) {}
			}
		}
	}

	private void renderTo(Target target, Boolean renderSides, Boolean attachmentSurfaces) {

		double baseEle = data.getBuildingPart().getBuilding().getGroundLevelEle();

		Material material = BuildingPart.buildMaterial(data.getTags().getValue("material"), null, Materials.BRICK, false);

		for (Integer level : data.getRenderableLevels()) {

			AttachmentSurface.Builder builder = new AttachmentSurface.Builder("wall" + level, "wall");
			boolean somethingRendered = false;

			if (attachmentSurfaces) {
				target = builder;
			}

			double ceilingHeight = baseEle + data.getBuildingPart().levelStructure.level(level).relativeEleTop();
			double floorHeight = baseEle + data.getBuildingPart().levelStructure.level(level).relativeEle;

			for (SegmentNodes wallSegData : wallSegmentNodes) {

				SegmentLevelPair pair = new SegmentLevelPair(wallSegData.getSegment(), level, wallSegData.getStartNode(), wallSegData.getEndNode());

				if (!allRenderedWallSegments.contains(pair) || attachmentSurfaces) {

					if (floorHeight < ceilingHeight) {

						if (!attachmentSurfaces) {
							allRenderedWallSegments.add(pair);
						}

						List<VectorXZ> endPoints = getNewEndPoints(wallSegData, level, baseEle
								+ data.getBuildingPart().levelStructure.level(level).relativeEle, ceilingHeight);

						/* front wall surface */

						List<VectorXZ> bottomPointsXZ = new ArrayList<>(asList(endPoints.get(3), endPoints.get(0)));

						List<VectorXYZ> bottomPoints = new ArrayList<>(listXYZ(bottomPointsXZ,
								baseEle + data.getBuildingPart().levelStructure.level(level).relativeEle));

						List<VectorXYZ> topPoints = generateTopPoints(bottomPoints, ceilingHeight - topOffset);

						WallSurface mainSurface = new WallSurface(material, bottomPoints, topPoints);

						/* back wall surface */

						List<VectorXZ> backBottomPointsXZ = new ArrayList<>(asList(endPoints.get(2), endPoints.get(1)));

						Collections.reverse(backBottomPointsXZ);

						List<VectorXYZ> backBottomPoints = new ArrayList<>(listXYZ(backBottomPointsXZ,
								baseEle + data.getBuildingPart().levelStructure.level(level).relativeEle));

						List<VectorXYZ> backTopPoints = generateTopPoints(backBottomPoints, ceilingHeight - topOffset);

						WallSurface backSurface = new WallSurface(material, backBottomPoints, backTopPoints);


						/* generate wall edges */

						WallSurface rightSurface = null;
						WallSurface leftSurface = null;

						if (renderSides) {

							// TODO avoid needing a try

							try {

								List<VectorXZ> bottomVertexLoop = new ArrayList<>(endPoints);
								bottomVertexLoop.add(endPoints.get(0));

								SimplePolygonXZ bottomPolygonXZ = new SimplePolygonXZ(bottomVertexLoop);
								List<TriangleXYZ> bottomTriangles = TriangulationUtil.
										triangulate(bottomPolygonXZ.asPolygonWithHolesXZ())
										.stream()
										.map(t -> t.makeClockwise().xyz(baseEle + data.getBuildingPart().levelStructure.level(level).relativeEle))
										.collect(toList());

								List<TriangleXYZ> tempTopTriangles = TriangulationUtil.
										triangulate(bottomPolygonXZ.asPolygonWithHolesXZ())
										.stream()
										.map(t -> t.makeCounterclockwise().xyz(ceilingHeight - topOffset))
										.collect(toList());

								target.drawTriangles(defaultInnerMaterial, bottomTriangles, triangleTexCoordLists(bottomTriangles, material, GLOBAL_X_Z));
								target.drawTriangles(defaultInnerMaterial, tempTopTriangles, triangleTexCoordLists(tempTopTriangles, material, GLOBAL_X_Z));


								rightSurface = new WallSurface(material,
										asList(bottomPoints.get(1), backBottomPoints.get(0)),
										asList(topPoints.get(0), backTopPoints.get(backBottomPoints.size() - 1)));

								leftSurface = new WallSurface(material,
										asList(backBottomPoints.get(1), bottomPoints.get(0)),
										asList(backTopPoints.get(0), topPoints.get(topPoints.size() - 1)));

							} catch (InvalidGeometryException e) {
							}

						}

						/* add windows that aren't on vertices */

						LineSegmentXZ frontLineSegment = new LineSegmentXZ(endPoints.get(3), endPoints.get(0));
						LineSegmentXZ backLineSegment = new LineSegmentXZ(endPoints.get(1), endPoints.get(2));

						for (MapNode node : wallSegData.getNodes()) {

							Set<Integer> objectLevels = new HashSet<>();
							objectLevels.add(min(parseLevels(node.getTags().getValue("level"), singletonList(0))));
							objectLevels.addAll(parseLevels(node.getTags().getValue("repeat_on"), emptyList()));

							VectorXZ posFront = new VectorXZ(frontLineSegment.offsetOf(frontLineSegment.closestPoint(node.getPos())), 0);
							VectorXZ posback = new VectorXZ(backLineSegment.offsetOf(backLineSegment.closestPoint(node.getPos())), 0);

							if (objectLevels.contains(level)) {

								if (node.getTags().containsKey("window")
										&& !node.getTags().contains("window", "no")) {

									boolean transparent = data.getBuildingPart().getBuilding().queryWindowSegments(node, level);

									TagSet windowTags = inheritTags(node.getTags(), data.getTags());
									WindowParameters params = new WindowParameters(windowTags, data.getBuildingPart().levelStructure.level(level).height);

									GeometryWindow windowFront = new GeometryWindow(new VectorXZ(posFront.x, params.breast), params, transparent);
									GeometryWindow windowBack = new GeometryWindow(new VectorXZ(posback.x, params.breast), params, transparent);

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
							somethingRendered = true;
							mainSurface.renderTo(target, new VectorXZ(0, -floorHeight), false, null, !attachmentSurfaces);
							backSurface.renderTo(target, new VectorXZ(0, -floorHeight), false, null, !attachmentSurfaces);
							if (leftSurface != null && rightSurface != null) {
								rightSurface.renderTo(target, new VectorXZ(0, -floorHeight), false, null, true);
								leftSurface.renderTo(target, new VectorXZ(0, -floorHeight), false, null, true);
							}
						}
					} else {
						ConversionLog.warn("Zero height level for level " + level);
					}
				}
			}

			if (attachmentSurfaces && somethingRendered) {
				attachmentSurfacesList.add(builder.build());
			}

		}
	}

    @Override
    public void renderTo(Target target) {
    	renderTo(target, true, false);
    }

}
