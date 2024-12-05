package org.osm2world.core.world.modules.building;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.osm2world.core.ConversionFacade;
import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.map_data.creation.LatLon;
import org.osm2world.core.map_data.creation.MapDataBuilder;
import org.osm2world.core.map_data.creation.MetricMapProjection;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.TagSet;

public class BuildingModuleTest {

	@Test
	public void testRoofWithoutWalls() throws IOException {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File testFile = new File(classLoader.getResource("issue-203.osm").getFile());

		ConversionFacade facade = new ConversionFacade();
		Results results = facade.createRepresentations(testFile, null, null, null, null);

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
		new ConversionFacade().createRepresentations(new MetricMapProjection(new LatLon(0, 0)),
				mapData, null, null, null);

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
		new ConversionFacade().createRepresentations(new MetricMapProjection(new LatLon(0, 0)),
				mapData, null, null, null);

		if (!(buildingArea.getPrimaryRepresentation() instanceof Building building)) {
			fail();
		} else {
			assertEquals(3, building.getParts().size());
		}

	}

}
