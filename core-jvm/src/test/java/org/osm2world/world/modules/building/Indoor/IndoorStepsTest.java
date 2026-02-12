package org.osm2world.world.modules.building.Indoor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.osm2world.O2WTestConverter;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.MapWay;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.world.modules.RoadModule;

public class IndoorStepsTest {

	@Test
	public void testIndoorSteps_incline() throws IOException {

		var builder = new MapDataBuilder();

		builder.createWayArea(List.of(
				builder.createNode(-100, -100),
				builder.createNode(+100, -100),
				builder.createNode(+100, +100),
				builder.createNode(-100, +100)
		), TagSet.of("building", "yes", "building:levels", "3"));

		MapWay stepsWay = builder.createWay(List.of(
				builder.createNode(0, 0),
				builder.createNode(0, 10)
		), TagSet.of("highway", "steps", "level", "0;2", "incline", "down"));

		new O2WTestConverter().convert(builder.build(), null);

		var object = stepsWay.getWaySegments().get(0).getPrimaryRepresentation();

		if (!(object instanceof RoadModule.Road road)) {
			fail("Not a road: " + object);
		} else {
			assertEquals("floor2", road.getAttachmentConnectors().get(0).compatibleSurfaceTypes.get(0));
			assertEquals("floor0", road.getAttachmentConnectors().get(1).compatibleSurfaceTypes.get(0));
			/*
			// TODO: level is correctly written to attachment connectors, but there is nothing to attach to
			VectorXYZ start = road.getCenterline().get(0);
			VectorXYZ end = road.getCenterline().get(road.getCenterline().size() - 1);
			assertEquals(0.0, end.y, 1e-3);
			assertTrue(start.y > 1.0);
			*/
		}

	}

	@Test
	public void testIndoorSteps_levelOnNodes() throws IOException {

		var builder = new MapDataBuilder();

		builder.createWayArea(List.of(
				builder.createNode(-100, -100),
				builder.createNode(+100, -100),
				builder.createNode(+100, +100),
				builder.createNode(-100, +100)
		), TagSet.of("building", "yes", "building:levels", "3"));

		MapWay stepsWay = builder.createWay(List.of(
				builder.createNode(0, 0, TagSet.of("level", "1")),
				builder.createNode(0, 10, TagSet.of("level", "0"))
		), TagSet.of("highway", "steps", "level", "0;1"));

		new O2WTestConverter().convert(builder.build(), null);

		var object = stepsWay.getWaySegments().get(0).getPrimaryRepresentation();

		if (!(object instanceof RoadModule.Road road)) {
			fail("Not a road: " + object);
		} else {
			assertEquals("floor1", road.getAttachmentConnectors().get(0).compatibleSurfaceTypes.get(0));
			assertEquals("floor0", road.getAttachmentConnectors().get(1).compatibleSurfaceTypes.get(0));
			/*
			// TODO: level is correctly written to attachment connectors, but there is nothing to attach to
			VectorXYZ start = road.getCenterline().get(0);
			VectorXYZ end = road.getCenterline().get(road.getCenterline().size() - 1);
			assertEquals(0.0, end.y, 1e-3);
			assertTrue(start.y > 1.0);
			*/
		}

	}

}
