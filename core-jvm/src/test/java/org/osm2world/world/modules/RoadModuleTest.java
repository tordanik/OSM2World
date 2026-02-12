package org.osm2world.world.modules;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.osm2world.scene.material.DefaultMaterials.*;
import static org.osm2world.world.modules.RoadModule.findMatchingLanes;
import static org.osm2world.world.modules.RoadModule.getSurfaceForNode;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.MapData;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.world.modules.RoadModule.Lane;
import org.osm2world.world.modules.RoadModule.RoadPart;

public class RoadModuleTest {

	/** returns a test dataset representing a star-shaped junction of ways all meeting in a central shared node */
	private MapData createStarJunction(TagSet nodeTags, List<TagSet> wayTagSets) {
		var builder = new MapDataBuilder();
		MapNode centerNode = builder.createNode(0, 0, nodeTags);
		for (TagSet wayTags : wayTagSets) {
			var outerPos = VectorXZ.fromAngle(2 * PI * wayTagSets.indexOf(wayTags) / wayTagSets.size());
			MapNode outerNode = builder.createNode(outerPos.x, outerPos.z);
			builder.createWay(List.of(outerNode, centerNode), wayTags);
		}
		return builder.build();
	}

	@Test
	public void testGetSurfaceForNode() {

		MapData noPathData1 = createStarJunction(TagSet.of(), List.of(
				TagSet.of("highway", "residential", "surface", "concrete"),
				TagSet.of("highway", "residential", "surface", "asphalt"),
				TagSet.of("highway", "residential", "surface", "asphalt")));
		new RoadModule().applyTo(noPathData1);
		assertEquals(ASPHALT.defaultAppearance(),
				getSurfaceForNode(noPathData1.getMapNodes().stream().findAny().get()).get(new O2WConfig()));

		MapData noPathData = createStarJunction(TagSet.of(), List.of(
				TagSet.of("highway", "residential", "surface", "concrete"),
				TagSet.of("highway", "residential", "surface", "concrete"),
				TagSet.of("highway", "residential", "surface", "asphalt")));
		new RoadModule().applyTo(noPathData);
		assertEquals(CONCRETE.defaultAppearance(),
				getSurfaceForNode(noPathData.getMapNodes().stream().findAny().get()).get(new O2WConfig()));

		MapData mixedData = createStarJunction(TagSet.of(), List.of(
				TagSet.of("highway", "residential", "surface", "concrete"),
				TagSet.of("highway", "service", "surface", "asphalt"),
				TagSet.of("highway", "footway", "surface", "asphalt")));
		new RoadModule().applyTo(mixedData);
		assertEquals(CONCRETE.defaultAppearance(),
				getSurfaceForNode(mixedData.getMapNodes().stream().findAny().get()).get(new O2WConfig()));

		MapData allPathsData = createStarJunction(TagSet.of(), List.of(
				TagSet.of("highway", "path", "surface", "concrete"),
				TagSet.of("highway", "path", "surface", "asphalt"),
				TagSet.of("highway", "path", "surface", "asphalt")));
		new RoadModule().applyTo(allPathsData);
		assertEquals(ASPHALT.defaultAppearance(),
				getSurfaceForNode(allPathsData.getMapNodes().stream().findAny().get()).get(new O2WConfig()));

		MapData nodeTagData = createStarJunction(TagSet.of("surface", "gravel"), List.of(
				TagSet.of("highway", "residential", "surface", "concrete"),
				TagSet.of("highway", "residential", "surface", "asphalt"),
				TagSet.of("highway", "residential", "surface", "asphalt")));
		new RoadModule().applyTo(nodeTagData);
		assertEquals(GRAVEL.defaultAppearance(),
				getSurfaceForNode(nodeTagData.getMapNodes().stream().findAny().get()).get(new O2WConfig()));

	}

	@Test
	public void testFindMatchingLanes() {

		List<Lane> lanes1 = asList(
				new Lane(null, RoadModule.VEHICLE_LANE, RoadPart.LEFT, TagSet.of()),
				new Lane(null, RoadModule.KERB, RoadPart.LEFT, TagSet.of()),
				new Lane(null, RoadModule.SIDEWALK, RoadPart.LEFT, TagSet.of()));
		List<Lane> lanes2 = asList(
				new Lane(null, RoadModule.KERB, RoadPart.LEFT, TagSet.of()),
				new Lane(null, RoadModule.SIDEWALK, RoadPart.LEFT, TagSet.of()));

		Map<Integer, Integer> result = findMatchingLanes(lanes1, lanes2, true, false);

		assertEquals(2, result.size());
		assertEquals(0, (int)result.get(1));
		assertEquals(1, (int)result.get(2));

	}

	@Test
	public void testGetTagsWithPrefix() {

		TagSet tags = TagSet.of(
				"highway", "tertiary",
				"sidewalk", "both",
				"sidewalk:left:width", "2 m",
				"sidewalk:right:width", "3 m",
				"sidewalk:left:kerb", "lowered",
				"sidewalk:left:kerb:width", "0.30");

		TagSet sidewalkResult = RoadModule.Road.getTagsWithPrefix(tags, "sidewalk:left:", null);

		assertEquals(3, sidewalkResult.size());
		assertTrue(sidewalkResult.contains("width", "2 m"));
		assertTrue(sidewalkResult.contains("kerb", "lowered"));
		assertTrue(sidewalkResult.contains("kerb:width", "0.30"));

		TagSet kerbResult = RoadModule.Road.getTagsWithPrefix(tags, "sidewalk:left:kerb", "kerb");

		assertEquals(2, kerbResult.size());
		assertTrue(kerbResult.contains("kerb", "lowered"));
		assertTrue(kerbResult.contains("kerb:width", "0.30"));

	}

}
