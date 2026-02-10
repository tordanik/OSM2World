package org.osm2world.world.modules;

import static org.junit.Assert.assertFalse;
import static org.osm2world.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.osm2world.O2WTestConverter;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.scene.Scene;
import org.osm2world.scene.material.TextureDataDimensions;
import org.osm2world.world.modules.SportsModule.Pitch.PitchTexFunction;

public class SportsModuleTest {

	@Test
	public void testPitchTexFunction() {

		PitchTexFunction texFunction = new PitchTexFunction(
				NULL_VECTOR, new VectorXZ(2, 0), new VectorXZ(0, 1), new TextureDataDimensions(1, 1));

		assertAlmostEquals(0, 0, texFunction.apply(List.of(new VectorXYZ(0, 0, 0))).get(0));
		assertAlmostEquals(0, 1, texFunction.apply(List.of(new VectorXYZ(2, 0, 0))).get(0));
		assertAlmostEquals(1, 0, texFunction.apply(List.of(new VectorXYZ(0, 0, 1))).get(0));
		assertAlmostEquals(1, 1, texFunction.apply(List.of(new VectorXYZ(2, 0, 1))).get(0));
		assertAlmostEquals(0, .5, texFunction.apply(List.of(new VectorXYZ(1, 0, 0))).get(0));
		assertAlmostEquals(2, 2, texFunction.apply(List.of(new VectorXYZ(4, 0, 2))).get(0));
		assertAlmostEquals(0, -1, texFunction.apply(List.of(new VectorXYZ(-2, 0, 0))).get(0));

		texFunction = new PitchTexFunction(NULL_VECTOR, new VectorXZ(0, -3), new VectorXZ(2, 0), new TextureDataDimensions(1, 1));

		assertAlmostEquals(0, 0, texFunction.apply(List.of(new VectorXYZ(0, 0, 0))).get(0));
		assertAlmostEquals(0, 1, texFunction.apply(List.of(new VectorXYZ(0, 0, -3))).get(0));
		assertAlmostEquals(1, 0, texFunction.apply(List.of(new VectorXYZ(2, 0, 0))).get(0));
		assertAlmostEquals(1, 1, texFunction.apply(List.of(new VectorXYZ(2, 0, -3))).get(0));

	}

	@Test
	public void testUntexturedPitch() throws IOException {

		MapDataBuilder builder = new MapDataBuilder();

		List<MapNode> wayNodes = List.of(
				builder.createNode(0, 0),
				builder.createNode(105, 0),
				builder.createNode(105, 68),
				builder.createNode(0, 68)
		);
		builder.createWayArea(wayNodes, TagSet.of("leisure", "pitch", "sport", "soccer"));

		Scene result = new O2WTestConverter().convert(builder.build(), null);

		assertFalse(result.getMeshes().isEmpty());

	}

}
