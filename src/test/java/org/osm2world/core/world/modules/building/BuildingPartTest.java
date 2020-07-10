package org.osm2world.core.world.modules.building;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.osm2world.core.map_data.data.overlaps.MapOverlapType.SHARE_SEGMENT;
import static org.osm2world.core.test.TestMapDataGenerator.addOverlapAA;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationFactory;
import org.junit.Test;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.map_data.data.overlaps.MapOverlapAA;
import org.osm2world.core.map_data.data.overlaps.MapOverlapType;
import org.osm2world.core.test.TestMapDataGenerator;

public class BuildingPartTest {

	@Test
	public void testSplitIntoWalls() {

		TestMapDataGenerator generator = new TestMapDataGenerator();

		List<MapNode> nodes = new ArrayList<>(asList(
				generator.createNode(-10, -5),
				generator.createNode(  0, -5),
				generator.createNode(+10, -5),
				generator.createNode(+10, +5),
				generator.createNode(-10, +5)));

		nodes.add(nodes.get(0));

		MapArea buildingPartArea = generator.createWayArea(nodes, TagSet.of("building", "yes"));

		/* test the basic case */

		List<Wall> result = BuildingPart.splitIntoWalls(buildingPartArea, null);

		assertEquals(4, result.size());

		assertEquals(nodes.subList(0, 3), result.get(0).getNodes());
		assertEquals(nodes.subList(2, 4), result.get(1).getNodes());
		assertEquals(nodes.subList(3, 5), result.get(2).getNodes());
		assertEquals(nodes.subList(4, 6), result.get(3).getNodes());

		/* add a building:wall=yes way and test again */

		MapWay wallWay = generator.createWay(asList(nodes.get(1), nodes.get(0), nodes.get(4)),
				TagSet.of("building:wall", "yes"));

		result = BuildingPart.splitIntoWalls(buildingPartArea, null);

		assertEquals(5, result.size());

		assertEquals(nodes.subList(0, 2), result.get(0).getNodes());
		assertEquals(wallWay, result.get(0).wallWay);
		assertEquals(nodes.subList(1, 3), result.get(1).getNodes());
		assertNull(result.get(1).wallWay);
		assertEquals(nodes.subList(2, 4), result.get(2).getNodes());
		assertNull(result.get(2).wallWay);
		assertEquals(nodes.subList(3, 5), result.get(3).getNodes());
		assertNull(result.get(3).wallWay);
		assertEquals(nodes.subList(4, 6), result.get(4).getNodes());
		assertEquals(wallWay, result.get(4).wallWay);

	}

	@Test
	public void testLevel(){

		TestMapDataGenerator generator = new TestMapDataGenerator();

		List<MapNode> nodes = new ArrayList<>(asList(
				generator.createNode(-10, -5),
				generator.createNode(  0, -5),
				generator.createNode(+10, -5),
				generator.createNode(+10, +5),
				generator.createNode(-10, +5)));

		nodes.add(nodes.get(0));

		MapArea buildingPartArea = generator.createWayArea(nodes, TagSet.of("building", "yes", "building:levels", "5", "height", "12.5"));
		Building building = new Building(buildingPartArea, new BaseConfiguration());
		BuildingPart buildingPart = building.getParts().get(0);

		assertEquals(5, buildingPart.getBuildingLevels());
		assertEquals(2.5 , buildingPart.getLevelHeight(1), 0.01);
		assertEquals(0, buildingPart.getLevelHeightAboveBase(0), 0.01);
		assertEquals(10, buildingPart.getLevelHeightAboveBase(4), 0.01);
		assertEquals(0, buildingPart.getLevelHeightAboveBase(5), 0.01);
		assertEquals(0, buildingPart.getLevelHeightAboveBase(-1), 0.01);
		assertEquals(0, buildingPart.getMinLevel());


		buildingPartArea = generator.createWayArea(nodes, TagSet.of("building", "yes", "building:levels", "5",
				"height", "12.5",
				"building:min_level", "2",
				"building:underground:levels", "3"));

		building = new Building(buildingPartArea, new BaseConfiguration());
		buildingPart = building.getParts().get(0);

		assertEquals(2, buildingPart.getMinLevel());


		buildingPartArea = generator.createWayArea(nodes, TagSet.of("building", "yes", "building:levels", "5",
				"building:levels:underground", "3",
				"roof:levels", "3",
				"roof:height", "6",
				"roof:shape", "gabled",
				"min_level", "5",
				"non_existent_levels", "8"));

		MapArea level1 = generator.createWayArea(nodes, TagSet.of("indoor", "level", "level", "6", "height", "1"));
		MapArea level2 = generator.createWayArea(nodes, TagSet.of("indoor", "level", "level", "10", "height", "5"));

		addOverlapAA(buildingPartArea, level1, SHARE_SEGMENT);
		addOverlapAA(buildingPartArea, level2, SHARE_SEGMENT);

		building = new Building(buildingPartArea, new BaseConfiguration());
		buildingPart = building.getParts().get(0);

		assertEquals(0, buildingPart.getMinLevel());
		assertEquals(1, buildingPart.getLevelHeight(-2), 0.01);
		assertEquals(5, buildingPart.getLevelHeight(1), 0.01);
		assertEquals(1.875, buildingPart.getLevelHeight(4), 0.01);
		assertEquals(2, buildingPart.getLevelHeight(6), 0.01);
		assertEquals(2, buildingPart.getLevelHeight(7), 0.01);

	}

}
