package org.osm2world.world.modules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.osm2world.O2WTestConverter;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.MapData;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.scene.Scene;
import org.osm2world.world.data.WorldObject;

public class ParkingModuleTest {

	@Test
	public void testSingleSpaceParking() throws IOException {

		var builder = new MapDataBuilder();

		var wayNodes = List.of(
				builder.createNode(-7.5, -5.0),
				builder.createNode(7.5, -5.0),
				builder.createNode(7.5, 5.0),
				builder.createNode(-7.5, 5.0)
		);
		builder.createWayArea(wayNodes, TagSet.of("amenity", "parking"));
		builder.createWayArea(wayNodes, TagSet.of("amenity", "parking_space"));

		MapData mapData = builder.build();

		var o2w = new O2WTestConverter();
		Scene scene = o2w.convert(mapData, null);

		WorldObject object = scene.getWorldObjects().iterator().next();

		if (!(object instanceof ParkingModule.SurfaceParking parking)) {
			fail("Not a parking object: " + object);
		} else {
			assertEquals(1, parking.parkingSpaces.size());
		}

	}

}