package org.osm2world.world.modules;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.osm2world.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.test.TestUtil.assertAlmostEquals;
import static org.osm2world.world.modules.PowerModule.RooftopSolarPanels.roughCommonDivisor;

import java.util.List;

import org.junit.Test;
import org.osm2world.ConversionFacade;
import org.osm2world.map_data.creation.MapDataBuilder;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.Tag;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.Angle;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.output.Output;
import org.osm2world.output.common.material.TextureDataDimensions;
import org.osm2world.output.statistics.StatisticsOutput;
import org.osm2world.output.statistics.StatisticsOutput.Stat;
import org.osm2world.world.creation.WorldModule;
import org.osm2world.world.modules.PowerModule.RooftopSolarPanels.PanelTexCoordFunction;

public class PowerModuleTest {

	@Test
	public void testRepeatedRendering() {

		/* create fake data */

		var mapDataBuilder = new MapDataBuilder();
		MapNode n1 = mapDataBuilder.createNode(0, 0, TagSet.of("power", "tower"));
		MapNode n2 = mapDataBuilder.createNode(111, 0, TagSet.of("power", "tower"));

		mapDataBuilder.createWay(List.of(n1, n2), TagSet.of(new Tag("power", "line"), new Tag("cables", "4")));

		/* render to multiple targets */

		ConversionFacade cf = new ConversionFacade();

		StatisticsOutput t1 = new StatisticsOutput();
		StatisticsOutput t2 = new StatisticsOutput();

		List<Output> outputs = asList(t1, t2);
		List<WorldModule> modules = singletonList(new PowerModule());

		cf.createRepresentations(null, mapDataBuilder.build(), modules, null, outputs);

		/* check whether the results are the same each time */

		for (Stat stat : Stat.values()) {
			assertEquals(t1.getGlobalCount(stat), t2.getGlobalCount(stat));
		}

	}

	@Test
	public void testPanelTexCoords() {

		TextureDataDimensions d = new TextureDataDimensions(1, 1);

		PanelTexCoordFunction t1 = new PanelTexCoordFunction(new VectorXZ(100, 100), Angle.ofDegrees(0), 5, 10, 1, 1, d);

		assertAlmostEquals(0, 0, t1.apply(new VectorXYZ(100, 30, 100)));
		assertAlmostEquals(1, 1, t1.apply(new VectorXYZ(105, 30, 110)));
		assertAlmostEquals(2, 0.5, t1.apply(new VectorXYZ(110, 30, 105)));

		PanelTexCoordFunction t2 = new PanelTexCoordFunction(NULL_VECTOR, Angle.ofDegrees(90), 5, 10, 1, 1, d);

		assertAlmostEquals(0, 0, t2.apply(new VectorXYZ(0, 0, 0)));
		assertAlmostEquals(1, 1, t2.apply(new VectorXYZ(10, 0, -5)));

		PanelTexCoordFunction t3 = new PanelTexCoordFunction(NULL_VECTOR, Angle.ofDegrees(90), 20, 20, 4, 2, d);

		assertAlmostEquals(0, 0, t3.apply(new VectorXYZ(0, 0, 0)));
		assertAlmostEquals(1, 1, t3.apply(new VectorXYZ(10, 0, -5)));

	}

	@Test
	public void testRoughCommonDivisor() {

		assertEquals(3, (int)roughCommonDivisor(9.0, asList(3.0, 6.0), 2.5));
		assertEquals(3, (int)roughCommonDivisor(9.0, asList(3.1, 5.9), 3.5));
		assertEquals(4, (int)roughCommonDivisor(12.0, asList(9.0, 5.9), 2.3));

	}

}
