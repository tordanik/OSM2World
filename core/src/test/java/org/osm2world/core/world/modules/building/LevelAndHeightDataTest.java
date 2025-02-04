package org.osm2world.core.world.modules.building;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.osm2world.core.math.algorithms.GeometryUtil.closeLoop;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Test;
import org.osm2world.core.conversion.O2WConfig;
import org.osm2world.core.map_data.creation.MapDataBuilder;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.world.modules.building.LevelAndHeightData.Level.LevelType;

public class LevelAndHeightDataTest {

	private static List<MapNode> createTestBuildingOutline(MapDataBuilder builder) {
		return closeLoop(
				builder.createNode(-10, -5),
				builder.createNode(0, -5),
				builder.createNode(+10, -5),
				builder.createNode(+10, +5),
				builder.createNode(-10, +5));
	}

	@Test
	public void testLevelBasics() {

		var builder = new MapDataBuilder();

		List<MapNode> nodes = createTestBuildingOutline(builder);

		MapArea buildingArea = builder.createWayArea(nodes, TagSet.of(
				"building", "yes",
				"building:levels", "5",
				"height", "12.5"));
		Building building = new Building(buildingArea, new O2WConfig());
		LevelAndHeightData levelStructure = building.getParts().get(0).levelStructure;

		assertEquals(12.5, levelStructure.height(), 0);
		assertEquals(12.5, levelStructure.heightWithoutRoof(), 0);
		assertEquals(0, levelStructure.bottomHeight(), 0);

		assertEquals(5, levelStructure.levels.size());
		assertFalse(levelStructure.hasLevel(5));
		assertFalse(levelStructure.hasLevel(-1));
		assertNull(levelStructure.level(5));
		assertNull(levelStructure.level(-1));
		IntStream.range(0, 5).forEach(it -> assertTrue(levelStructure.hasLevel(it)));

		levelStructure.levels.forEach(it -> assertEquals(2.5, it.height, 1e-4));

		assertEquals(0, levelStructure.levels.get(0).relativeEle, 1e-4);
		assertEquals(10, levelStructure.level(4).relativeEle, 1e-4);

		assertEquals(emptyList(), levelStructure.levels(EnumSet.of(LevelType.UNDERGROUND)));
		assertEquals(5, levelStructure.levels(EnumSet.of(LevelType.ABOVEGROUND)).size());
		assertEquals(emptyList(), levelStructure.levels(EnumSet.of(LevelType.ROOF)));

	}

	@Test
	public void testMinLevel() {

		var builder = new MapDataBuilder();

		List<MapNode> nodes = createTestBuildingOutline(builder);

		MapArea buildingArea = builder.createWayArea(nodes, TagSet.of(
				"building", "yes",
				"building:levels", "5",
				"height", "12.5",
				"building:min_level", "2"));

		Building building = new Building(buildingArea, new O2WConfig());
		LevelAndHeightData levelStructure = building.getParts().get(0).levelStructure;

		assertEquals(12.5, levelStructure.height(), 0);
		assertEquals(12.5, levelStructure.heightWithoutRoof(), 0);
		assertEquals(5, levelStructure.bottomHeight(), 0);

		assertEquals(3, levelStructure.levels.size());
		assertFalse(levelStructure.hasLevel(5));
		assertFalse(levelStructure.hasLevel(1));
		assertNull(levelStructure.level(5));
		assertNull(levelStructure.level(1));
		IntStream.range(2, 5).forEach(it -> assertTrue(levelStructure.hasLevel(it)));

		levelStructure.levels.forEach(it -> assertEquals(2.5, it.height, 1e-4));

	}

	@Test
	public void testSITLevelsAndRoof() {

		var builder = new MapDataBuilder();

		List<MapNode> nodes = createTestBuildingOutline(builder);

		MapArea buildingArea = builder.createWayArea(nodes, TagSet.of(
				"building", "yes",
				"building:levels", "5",
				"building:levels:underground", "3",
				"height", "19",
				"roof:levels", "3",
				"roof:height", "6",
				"roof:shape", "gabled",
				"min_level", "5",
				"max_level", "16",
				"non_existent_levels", "8"));

		builder.createWayArea(nodes, TagSet.of("indoor", "level", "level", "7", "height", "1"));
		builder.createWayArea(nodes, TagSet.of("indoor", "level", "level", "10", "height", "5"));
		builder.createWayArea(nodes, TagSet.of("indoor", "level", "level", "11", "ref", "L11"));

		Building building = new Building(buildingArea, new O2WConfig());
		LevelAndHeightData levelStructure = building.getParts().get(0).levelStructure;

		assertEquals(19, levelStructure.height(), 0);
		assertEquals(13, levelStructure.heightWithoutRoof(), 0);
		assertEquals(0, levelStructure.bottomHeight(), 0);

		assertEquals(11, levelStructure.levels.size());
		assertEquals(3, levelStructure.levels(EnumSet.of(LevelType.UNDERGROUND)).size());
		assertEquals(5, levelStructure.levels(EnumSet.of(LevelType.ABOVEGROUND)).size());
		assertEquals(3, levelStructure.levels(EnumSet.of(LevelType.ROOF)).size());
		assertFalse(levelStructure.hasLevel(8));

		IntStream.range(5, 8).forEach(it -> assertTrue(levelStructure.hasLevel(it)));
		IntStream.range(9, 17).forEach(it -> assertTrue(levelStructure.hasLevel(it)));

		assertEquals("L11", levelStructure.level(11).ref);
		assertEquals("L11", levelStructure.level(11).label());

		assertEquals(1, levelStructure.level(7).height, 0);
		assertEquals(5, levelStructure.level(10).height, 0);
		assertEquals(2, levelStructure.level(11).height, 0);
		assertEquals(2, levelStructure.level(15).height, 0);

		assertEquals(-1, levelStructure.level(7).relativeEle, 0);

	}

	@Test
	public void testSingleLevel() {

		var builder = new MapDataBuilder();

		List<MapNode> nodes = createTestBuildingOutline(builder);

		MapArea buildingArea = builder.createWayArea(nodes, TagSet.of(
				"building", "yes",
				"building:levels", "1",
				"min_level", "42",
				"max_level", "42"));
		Building building = new Building(buildingArea, new O2WConfig());
		LevelAndHeightData levelStructure = building.getParts().get(0).levelStructure;

		assertEquals(1, levelStructure.levels.size());
		assertTrue(levelStructure.hasLevel(42));
		assertEquals(0, levelStructure.level(42).relativeEle, 0);

	}

	@Test
	public void testRoofLevelsOnly() {

		var builder = new MapDataBuilder();

		List<MapNode> nodes = createTestBuildingOutline(builder);

		MapArea buildingArea = builder.createWayArea(nodes, TagSet.of(
				"building", "yes",
				"building:levels", "0",
				"roof:levels", "2",
				"roof:shape", "gabled",
				"height", "8 m"));
		Building building = new Building(buildingArea, new O2WConfig());
		LevelAndHeightData levelStructure = building.getParts().get(0).levelStructure;

		assertEquals(8, levelStructure.height(), 0);
		assertEquals(0, levelStructure.heightWithoutRoof(), 0);
		assertEquals(0, levelStructure.bottomHeight(), 0);

		assertEquals(2, levelStructure.levels.size());
		assertEquals(emptyList(), levelStructure.levels(EnumSet.of(LevelType.ABOVEGROUND)));
		assertEquals(2, levelStructure.levels(EnumSet.of(LevelType.ROOF)).size());

		IntStream.range(0, 2).forEach(it -> assertTrue(levelStructure.hasLevel(it)));
		IntStream.range(0, 2).forEach(it -> assertSame(LevelType.ROOF, levelStructure.level(it).type));

		assertEquals(4, levelStructure.level(1).height, 0);

		// same as above, but with no tagged height

		buildingArea = builder.createWayArea(nodes, TagSet.of(
				"building", "yes",
				"building:levels", "0",
				"roof:levels", "2",
				"roof:shape", "gabled"));
		building = new Building(buildingArea, new O2WConfig());
		LevelAndHeightData result2 = building.getParts().get(0).levelStructure;

		assertEquals(0, result2.heightWithoutRoof(), 0);
		assertEquals(0, result2.bottomHeight(), 0);

		assertEquals(2, result2.levels.size());
		assertEquals(emptyList(), result2.levels(EnumSet.of(LevelType.ABOVEGROUND)));
		assertEquals(2, result2.levels(EnumSet.of(LevelType.ROOF)).size());

		IntStream.range(0, 2).forEach(it -> assertTrue(result2.hasLevel(it)));
		IntStream.range(0, 2).forEach(it -> assertSame(LevelType.ROOF, result2.level(it).type));

		assertEquals(result2.height() / 2, result2.level(1).height, 0);

	}

	@Test
	public void testUndergroundLevelsOnly() {

		var builder = new MapDataBuilder();

		List<MapNode> nodes = createTestBuildingOutline(builder);

		MapArea buildingArea = builder.createWayArea(nodes, TagSet.of(
				"building", "yes",
				"building:levels", "0",
				"building:levels:underground", "2",
				"max_level", "-1"));
		Building building = new Building(buildingArea, new O2WConfig());
		LevelAndHeightData levelStructure = building.getParts().get(0).levelStructure;

		assertEquals(0, levelStructure.height(), 0);
		assertEquals(0, levelStructure.heightWithoutRoof(), 0);
		assertEquals(0, levelStructure.bottomHeight(), 0);

		assertEquals(2, levelStructure.levels.size());
		assertEquals(emptyList(), levelStructure.levels(EnumSet.of(LevelType.ABOVEGROUND, LevelType.ROOF)));
		assertEquals(2, levelStructure.levels(EnumSet.of(LevelType.UNDERGROUND)).size());

		IntStream.range(-2, 0).forEach(it -> assertTrue(levelStructure.hasLevel(it)));

	}

	@Test
	public void testSITMinLevelWithoutMaxLevel() {

		var builder = new MapDataBuilder();

		List<MapNode> nodes = createTestBuildingOutline(builder);

		MapArea buildingPartArea = builder.createWayArea(nodes, TagSet.of(
				"building", "yes",
				"building:levels", "3",
				"building:levels:underground", "1",
				"min_level", "7",
				"non_existent_levels", "8;11"));

		Building building = new Building(buildingPartArea, new O2WConfig());
		LevelAndHeightData levelStructure = building.getParts().get(0).levelStructure;

		assertEquals(1, levelStructure.levels(EnumSet.of(LevelType.UNDERGROUND)).size());
		assertEquals(7, levelStructure.levels(EnumSet.of(LevelType.UNDERGROUND)).get(0).level);

		assertEquals(9, levelStructure.levels.get(1).level);
		assertEquals(10, levelStructure.levels.get(2).level);
		assertEquals(12, levelStructure.levels.get(3).level);

	}

	@Test
	public void testExplicitLevelHeightsDoNotMatchTotalHeight() {

		var builder = new MapDataBuilder();

		List<MapNode> nodes = createTestBuildingOutline(builder);

		MapArea buildingArea = builder.createWayArea(nodes, TagSet.of(
				"building", "yes",
				"building:levels", "2",
				"height", "5"));

		// explicit heights conflict with total height, should therefore be ignored
		builder.createWayArea(nodes, TagSet.of("indoor", "level", "level", "0", "height", "4"));
		builder.createWayArea(nodes, TagSet.of("indoor", "level", "level", "1", "height", "6"));

		Building building = new Building(buildingArea, new O2WConfig());
		LevelAndHeightData levelStructure = building.getParts().get(0).levelStructure;

		assertEquals(5, levelStructure.height(), 0);
		assertEquals(5, levelStructure.heightWithoutRoof(), 0);
		assertEquals(0, levelStructure.bottomHeight(), 0);

		assertEquals(2, levelStructure.levels.size());

		assertEquals(2.5, levelStructure.level(0).height, 0);
		assertEquals(2.5, levelStructure.level(1).height, 0);

	}

	@Test
	public void testInvalidMinHeight() {

		var builder = new MapDataBuilder();

		List<MapNode> nodes = createTestBuildingOutline(builder);

		MapArea buildingArea = builder.createWayArea(nodes, TagSet.of(
				"building", "yes",
				"height", "5",
				"roof:height", "2",
				"min_height", "4"));

		Building building = new Building(buildingArea, new O2WConfig());
		LevelAndHeightData levelStructure = building.getParts().get(0).levelStructure;

		assertEquals(5, levelStructure.height(), 5);
		assertEquals(5, levelStructure.heightWithoutRoof(), 3);

	}


	@Test
	public void testRoofLevelAndMinLevel() {

		var builder = new MapDataBuilder();

		List<MapNode> nodes = createTestBuildingOutline(builder);

		MapArea buildingArea = builder.createWayArea(nodes, TagSet.of(
				"building", "yes",
				"building:levels", "3",
				"building:min_level", "2",
				"roof:levels", "1",
				"roof:shape", "round"));
		Building building = new Building(buildingArea, new O2WConfig());
		LevelAndHeightData levelStructure = building.getParts().get(0).levelStructure;

		assertEquals(2, levelStructure.levels.size());
		assertEquals(emptyList(), levelStructure.levels(EnumSet.of(LevelType.UNDERGROUND)));
		assertEquals(1, levelStructure.levels(EnumSet.of(LevelType.ABOVEGROUND)).size());
		assertEquals(1, levelStructure.levels(EnumSet.of(LevelType.ROOF)).size());

		assertEquals(2, levelStructure.levels(EnumSet.of(LevelType.ABOVEGROUND)).get(0).level);
		assertEquals(3, levelStructure.levels(EnumSet.of(LevelType.ROOF)).get(0).level);

		assertTrue(levelStructure.height() > levelStructure.heightWithoutRoof());

	}

}
