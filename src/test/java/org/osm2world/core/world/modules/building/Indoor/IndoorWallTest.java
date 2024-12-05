package org.osm2world.core.world.modules.building.Indoor;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.osm2world.core.math.GeometryUtil.closeLoop;

import java.util.List;

import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Test;
import org.osm2world.core.map_data.creation.MapDataBuilder;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.world.modules.building.Building;
import org.osm2world.core.world.modules.building.BuildingPart;
import org.osm2world.core.world.modules.building.indoor.IndoorObjectData;
import org.osm2world.core.world.modules.building.indoor.IndoorWall;

public class IndoorWallTest {

	@Test
	public void testGetNewEndPoints(){

		var builder = new MapDataBuilder();

		/* generate building part */

		List<MapNode> buildingNodes = closeLoop(
				builder.createNode(-10, -5),
				builder.createNode(  0, -5),
				builder.createNode(+10, -5),
				builder.createNode(+10, +5),
				builder.createNode(-10, +5));

		MapArea buildingPartArea = builder.createWayArea(buildingNodes, TagSet.of("building", "yes", "building:levels", "5", "height", "12.5"));
		Building building = new Building(buildingPartArea, new BaseConfiguration());
		BuildingPart buildingPart = building.getParts().get(0);

		/* generate wall */

		List<MapNode> wallNodes = List.of(
				builder.createNode(-9, -4),
				builder.createNode(+9, -4),
				builder.createNode( 0, 4),
				builder.createNode( 0, -3));

		MapWay wallWay = builder.createWay(List.of(wallNodes.get(0), wallNodes.get(2)), TagSet.of("indoor", "wall", "level", "2"));
		LineSegmentXZ wallWayLineSegment = new LineSegmentXZ(wallNodes.get(0).getPos(), wallNodes.get(2).getPos());

		VectorXZ startNodePos = wallNodes.get(0).getPos();
		VectorXZ endNodePos = wallNodes.get(2).getPos();

		IndoorWall wall = new IndoorWall(new IndoorObjectData(buildingPart, wallWay.getWaySegments().get(0)));

		/* test basic case */

		List<VectorXZ> endPointsList = wall.getNewEndPoints(wall.getWallSegmentNodes().get(0),2, 5, 7.5);

		VectorXZ wallWayRightNormal = wallWayLineSegment.getDirection().rightNormal();

		assertEquals(startNodePos.add(wallWayRightNormal.mult(-wall.getWallThickness())), endPointsList.get(0));
		assertEquals(startNodePos.add(wallWayRightNormal.mult(wall.getWallThickness())), endPointsList.get(1));
		assertEquals(endNodePos.add(wallWayRightNormal.mult(wall.getWallThickness())), endPointsList.get(2));
		assertEquals(endNodePos.add(wallWayRightNormal.mult(-wall.getWallThickness())), endPointsList.get(3));

		/* test intersection of 2 line segments */

		MapWay secondWallWay = builder.createWay(asList(wallNodes.get(2), wallNodes.get(1)), TagSet.of("indoor", "wall", "level", "2"));
		LineSegmentXZ secondWallWayLineSegment = new LineSegmentXZ(wallNodes.get(2).getPos(), wallNodes.get(1).getPos());

		VectorXZ secondStartNodePos = wallNodes.get(2).getPos();
		VectorXZ secondEndNodePos = wallNodes.get(1).getPos();
		VectorXZ secondRightNormal = secondWallWayLineSegment.getDirection().rightNormal();

		IndoorWall secondWall = new IndoorWall(new IndoorObjectData(buildingPart, secondWallWay.getWaySegments().get(0)));


		List<VectorXZ> secondEndPointsList = secondWall.getNewEndPoints(secondWall.getWallSegmentNodes().get(0),2, 5, 7.5);

		double offset = secondWall.getWallThickness() / Math.sin(PI/2);

		assertEquals(0, new VectorXZ(0, 4 - offset).subtract(secondEndPointsList.get(1)).length(), 0.05);
		assertEquals(0, new VectorXZ(0, 4 + offset).subtract(secondEndPointsList.get(0)).length(), 0.05);
		assertEquals(secondEndNodePos.add(secondRightNormal.mult(wall.getWallThickness())), secondEndPointsList.get(2));
		assertEquals(secondEndNodePos.add(secondRightNormal.mult(-wall.getWallThickness())), secondEndPointsList.get(3));

		endPointsList = wall.getNewEndPoints(wall.getWallSegmentNodes().get(0),2, 5, 7.5);

		assertEquals(0, endPointsList.get(3).subtract(secondEndPointsList.get(0)).length(), 0.05);
		assertEquals(0, endPointsList.get(2).subtract(secondEndPointsList.get(1)).length(), 0.05);

		/* test intersections for line not on the same level */

		MapWay thirdWallWay = builder.createWay(asList(wallNodes.get(2), wallNodes.get(3)), TagSet.of("indoor", "wall", "level", "3"));
		LineSegmentXZ thirdWallWayLineSegment = new LineSegmentXZ(wallNodes.get(2).getPos(), wallNodes.get(3).getPos());

		VectorXZ thirdStartNodePos = wallNodes.get(2).getPos();
		VectorXZ thirdEndNodePos = wallNodes.get(3).getPos();
		VectorXZ thirdRightNormal = thirdWallWayLineSegment.getDirection().rightNormal();

		IndoorWall thirdWall = new IndoorWall(new IndoorObjectData(buildingPart, thirdWallWay.getWaySegments().get(0)));

		endPointsList = wall.getNewEndPoints(wall.getWallSegmentNodes().get(0),2, 5, 7.5);
		secondEndPointsList = secondWall.getNewEndPoints(secondWall.getWallSegmentNodes().get(0),2, 5, 7.5);
		List<VectorXZ> thirdEndPointsList = thirdWall.getNewEndPoints(thirdWall.getWallSegmentNodes().get(0),3, 7.5, 10);

		assertEquals(0, endPointsList.get(3).subtract(secondEndPointsList.get(0)).length(), 0.05);
		assertEquals(0, endPointsList.get(2).subtract(secondEndPointsList.get(1)).length(), 0.05);

		assertEquals(thirdStartNodePos.add(thirdRightNormal.mult(-thirdWall.getWallThickness())), thirdEndPointsList.get(0));
		assertEquals(thirdStartNodePos.add(thirdRightNormal.mult(thirdWall.getWallThickness())), thirdEndPointsList.get(1));
		assertEquals(thirdEndNodePos.add(thirdRightNormal.mult(thirdWall.getWallThickness())), thirdEndPointsList.get(2));
		assertEquals(thirdEndNodePos.add(thirdRightNormal.mult(-thirdWall.getWallThickness())), thirdEndPointsList.get(3));

	}

}
