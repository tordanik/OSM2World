package org.osm2world.world.modules.building.roof;

import static org.osm2world.test.TestUtil.assertAlmostEquals;

import java.util.List;

import org.junit.Test;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;

public class SideHippedRoofTest {

	@Test
	public void testDetermineDefaultDirection() {

		var builder = new MapDataBuilder();

		var nodes1 = List.of(
			builder.createNode(-10, -5),
			builder.createNode(+10, -5),
			builder.createNode(+10, -3),
			builder.createNode(+10, +5),
			builder.createNode(-10, +5)
		);

		var nodes2 = List.of(
				nodes1.get(1),
				nodes1.get(2),
				nodes1.get(3),
				builder.createNode(+25, +5),
				builder.createNode(+25, -5)
		);

		TagSet tags = TagSet.of(
				"building", "yes",
				"roof:shape", "side_hipped"
		);

		var area1 = builder.createWayArea(nodes1, tags);
		var area2 = builder.createWayArea(nodes2, tags);

		assertAlmostEquals(new VectorXZ(-1, 0), SideHippedRoof.determineDefaultDirection(area1));
		assertAlmostEquals(new VectorXZ(+1, 0), SideHippedRoof.determineDefaultDirection(area2));

	}

}
