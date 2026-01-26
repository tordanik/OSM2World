package org.osm2world.world.modules.building;

import static org.junit.Assert.*;
import static org.osm2world.util.test.TestFileUtil.getTestFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.osm2world.O2WConverter;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.MapData;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.osm.creation.OSMFileReader;
import org.osm2world.scene.Scene;
import org.osm2world.scene.material.Materials;
import org.osm2world.util.test.TestFileUtil;

import com.google.common.collect.Lists;

public class BuildingModuleTest {

	@Test
	public void testRoofWithoutWalls() throws IOException {

		File testFile = getTestFile("issue-203.osm");

		Scene results = new O2WConverter().convert(new OSMFileReader(testFile), null, null);

		Collection<MapArea> areas = results.getMapData().getMapAreas();
		assertEquals(5, areas.size());

		MapArea buildingArea = areas.stream().filter(a -> a.getId() == 103224).findFirst().get();
		assertTrue(buildingArea.getPrimaryRepresentation() instanceof Building);
		Building building = (Building) buildingArea.getPrimaryRepresentation();
		assertEquals(4, building.getParts().size());

		for (BuildingPart part : building.getParts()) {
			part.buildMeshes();
		}

	}

	@Test
	public void testBuildingWithOverhang() throws IOException {

		var builder = new MapDataBuilder();

		List<MapNode> nodes = List.of(
				builder.createNode(0, 0),
				builder.createNode(10, 0),
				builder.createNode(15, 0),
				builder.createNode(15, 10),
				builder.createNode(10, 10),
				builder.createNode(0, 10)
		);

		MapArea buildingArea = builder.createWayArea(
				List.of(nodes.get(0), nodes.get(1), nodes.get(4), nodes.get(5)),
				TagSet.of("building", "yes"));

		MapArea partBottom = builder.createWayArea(
				buildingArea.getBoundaryNodes(),
				TagSet.of("building:part", "yes", "building:levels", "1"));
		MapArea partTop = builder.createWayArea(
				List.of(nodes.get(0), nodes.get(2), nodes.get(3), nodes.get(4)),
				TagSet.of("building:part", "yes", "building:levels", "2", "building:min_level", "1"));

		builder.createRelation(List.of(
				Map.entry("outline", buildingArea),
				Map.entry("part", partBottom),
				Map.entry("part", partTop)
		), TagSet.of("type", "building"));

		MapData mapData = builder.build();
		new O2WConverter().convert(mapData, null);

		if (!(buildingArea.getPrimaryRepresentation() instanceof Building building)) {
			fail();
		} else {
			assertEquals(2, building.getParts().size());
		}

	}

	@Test
	public void testBuildingWithPartOutsideOutline() throws IOException {

		var builder = new MapDataBuilder();

		List<MapNode> nodes = List.of(
				builder.createNode(0, 0),
				builder.createNode(10, 0),
				builder.createNode(15, 0),
				builder.createNode(20, 0),
				builder.createNode(20, 10),
				builder.createNode(15, 10),
				builder.createNode(10, 10),
				builder.createNode(0, 10)
		);

		MapArea buildingArea = builder.createWayArea(
				List.of(nodes.get(0), nodes.get(1), nodes.get(6), nodes.get(7)),
				TagSet.of("building", "yes"));

		MapArea partBottom = builder.createWayArea(
				buildingArea.getBoundaryNodes(),
				TagSet.of("building:part", "yes", "building:levels", "1"));
		MapArea partTop1 = builder.createWayArea(
				List.of(nodes.get(0), nodes.get(2), nodes.get(5), nodes.get(7)),
				TagSet.of("building:part", "yes", "building:levels", "2", "building:min_level", "1"));
		MapArea partTop2 = builder.createWayArea(
				List.of(nodes.get(2), nodes.get(3), nodes.get(4), nodes.get(5)),
				TagSet.of("building:part", "yes", "building:levels", "2", "building:min_level", "1"));

		builder.createRelation(List.of(
				Map.entry("outline", buildingArea),
				Map.entry("part", partBottom),
				Map.entry("part", partTop1),
				Map.entry("part", partTop2)
		), TagSet.of("type", "building"));

		MapData mapData = builder.build();
		new O2WConverter().convert(mapData, null);

		if (!(buildingArea.getPrimaryRepresentation() instanceof Building building)) {
			fail();
		} else {
			assertEquals(3, building.getParts().size());
		}

	}

	@Test
	public void testMultipleOuters() throws IOException {

		File testFile = TestFileUtil.getTestFile("mp_two_outer_roof.osm");

		O2WConverter o2w = new O2WConverter();
		Scene scene = o2w.convert(new OSMFileReader(testFile), null, null);

		ArrayList<Building> buildings = Lists.newArrayList(scene.getWorldObjects(Building.class));

		assertSame(1, buildings.size());

		assertTrue(buildings.get(0).buildMeshes().stream().anyMatch(it ->
				Materials.ROOF_DEFAULT.get(config).equals(it.material)));

		assertFalse(scene.getMeshes().isEmpty());

	}

}
