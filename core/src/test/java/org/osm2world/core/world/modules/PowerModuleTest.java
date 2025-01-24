package org.osm2world.core.world.modules;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.test.TestUtil.assertAlmostEquals;
import static org.osm2world.core.world.modules.PowerModule.RooftopSolarPanels.roughCommonDivisor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.osm2world.core.ConversionFacade;
import org.osm2world.core.math.Angle;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.TextureDataDimensions;
import org.osm2world.core.target.statistics.StatisticsTarget;
import org.osm2world.core.target.statistics.StatisticsTarget.Stat;
import org.osm2world.core.world.creation.WorldModule;
import org.osm2world.core.world.modules.PowerModule.RooftopSolarPanels.PanelTexCoordFunction;

import com.slimjars.dist.gnu.trove.list.TLongList;
import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;

import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.core.model.impl.Way;

public class PowerModuleTest {

	@Test
	public void testRepeatedRendering() throws Exception {

		/* create fake data */

		List<Node> nodes = new ArrayList<Node>();
		nodes.add(new Node(101, 0, 0));
		nodes.add(new Node(102, 0.001, 0));

		nodes.get(0).setTags(asList(new Tag("power", "tower")));
		nodes.get(1).setTags(asList(new Tag("power", "tower")));

		TLongList nodeIds = new TLongArrayList(new long[] {101, 102});

		Way way = new Way(201, nodeIds);
		way.setTags(asList(new Tag("power", "line"), new Tag("cables","4")));
		List<Way> ways = Collections.singletonList(way);

		OSMData osmData = new OSMData(emptyList(), nodes, ways, emptyList());

		/* render to multiple targets */

		ConversionFacade cf = new ConversionFacade();

		StatisticsTarget t1 = new StatisticsTarget();
		StatisticsTarget t2 = new StatisticsTarget();

		List<Target> targets = asList(t1, t2);
		List<WorldModule> modules = singletonList(new PowerModule());

		cf.createRepresentations(osmData, null, modules, null, targets);

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
