package org.osm2world.map_data.creation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.osm2world.map_data.data.*;
import org.osm2world.map_data.data.overlaps.MapOverlap;
import org.osm2world.map_data.data.overlaps.MapOverlapType;

public class MapDataBuilderTest {

	@Test
	public void testMultipolygonArea() {

		var builder = new MapDataBuilder();
		
		List<MapNode> outerRing = List.of(
			builder.createNode(-10, -10),
			builder.createNode(10, -10),
			builder.createNode(10, 10),
			builder.createNode(-10, 10)
		);

		List<List<MapNode>> innerRings = List.of(
				List.of(
						builder.createNode(-5, -5),
						builder.createNode(5, -5),
						builder.createNode(5, 5),
						builder.createNode(-5, 5)
				)
		);

		MapArea area1 = builder.createMultipolygonArea(outerRing, innerRings, TagSet.of("highway", "pedestrian"));
		MapArea area2 = (MapArea)builder.createMultipolygon(List.of(outerRing), innerRings, TagSet.of("highway", "pedestrian"));

		assertEquals(300.0, area1.getPolygon().getArea(), 0.1);
		assertEquals(300.0, area2.getPolygon().getArea(), 0.1);
		
	}

	@Test
	public void testMultipolygonMultipleOuters() {

		var builder = new MapDataBuilder();

		List<List<MapNode>> outerRings = List.of(
				List.of(
						builder.createNode(-10, -10),
						builder.createNode(10, -10),
						builder.createNode(10, 10),
						builder.createNode(-10, 10)
				),
				List.of(
						builder.createNode(-10, -10 + 100),
						builder.createNode(10, -10 + 100),
						builder.createNode(10, 10 + 100),
						builder.createNode(-10, 10 + 100)
				),
				List.of(
						builder.createNode(-2, -2),
						builder.createNode(2, -2),
						builder.createNode(2, 2),
						builder.createNode(-2, 2)
				)
		);

		List<List<MapNode>> innerRings = List.of(
				List.of(
						builder.createNode(-5, -5),
						builder.createNode(5, -5),
						builder.createNode(5, 5),
						builder.createNode(-5, 5)
				),
				List.of(
						builder.createNode(-1, -1),
						builder.createNode(1, -1),
						builder.createNode(1, 1),
						builder.createNode(-1, 1)
				),
				List.of(
						builder.createNode(-5, -5 + 100),
						builder.createNode(5, -5 + 100),
						builder.createNode(5, 5 + 100),
						builder.createNode(-5, 5 + 100)
				)
		);

		MapMultipolygonRelation relation = (MapMultipolygonRelation)builder.createMultipolygon(outerRings, innerRings,
				TagSet.of("type", "multipolygon", "amenity", "parking"));

		assertEquals(3, relation.getAreas().size());
		assertEquals(300.0, relation.getAreas().get(0).getPolygon().getArea(), 0.1);
		assertEquals(300.0, relation.getAreas().get(1).getPolygon().getArea(), 0.1);
		assertEquals( 12.0, relation.getAreas().get(2).getPolygon().getArea(), 0.1);

	}

	@Test
	public void testRelationWithOneElement() {
		var builder = new MapDataBuilder();
		var node = builder.createNode(42, -24);
		var relation = builder.createRelation(List.of(Map.entry("", node)), TagSet.of("type", "something"));
		assertEquals(List.of(relation), getBuild(builder).getMapRelations());
	}

	private static MapData getBuild(MapDataBuilder builder) {
		return builder.build();
	}

	@Test
	public void testIntersections() {

		var builder = new MapDataBuilder();

		MapArea area = builder.createWayArea(List.of(
				builder.createNode(-10, -10),
				builder.createNode(10, -10),
				builder.createNode(10, 10),
				builder.createNode(-10, 10)
		), TagSet.of());

		var way1 = builder.createWay(List.of(
				builder.createNode(-8, 0),
				builder.createNode(18, 0)
		), TagSet.of());

		var way2 = builder.createWay(List.of(
				builder.createNode(0, -5),
				builder.createNode(0,  5)
		), TagSet.of());

		var mapData = builder.build();

		assertOverlap(mapData, area, way1.getNodes().get(0), MapOverlapType.CONTAIN);
		assertOverlap(mapData, area, way1.getWaySegments().get(0), MapOverlapType.INTERSECT);
		assertOverlap(mapData, area, way2.getWaySegments().get(0), MapOverlapType.CONTAIN);
		assertOverlap(mapData, way1.getWaySegments().get(0), way2.getWaySegments().get(0), MapOverlapType.INTERSECT);

	}

	private void assertOverlap(MapData mapData, MapElement e1, MapElement e2, MapOverlapType type) {

		Set<MapOverlap<?, ?>> overlaps = new HashSet<>();
		mapData.getMapElements().forEach(e -> overlaps.addAll(e.getOverlaps()));

		assertTrue(overlaps.stream().anyMatch(o -> o.type == type
				&& (o.e1 == e1 && o.e2 == e2 || o.e1 == e2 && o.e2 == e1)));

	}

}