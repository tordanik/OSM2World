package org.osm2world.core.world.modules;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.osm2world.core.ConversionFacade;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.statistics.StatisticsTarget;
import org.osm2world.core.target.statistics.StatisticsTarget.Stat;
import org.osm2world.core.world.creation.WorldModule;

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

		List<Target<?>> targets = Arrays.<Target<?>>asList(t1, t2);
		List<WorldModule> modules = Collections.<WorldModule>singletonList(new PowerModule());

		cf.createRepresentations(osmData, modules, null, targets);

		/* check whether the results are the same each time */

		for (Stat stat : Stat.values()) {
			assertEquals(t1.getGlobalCount(stat), t2.getGlobalCount(stat));
		}

	}

}
