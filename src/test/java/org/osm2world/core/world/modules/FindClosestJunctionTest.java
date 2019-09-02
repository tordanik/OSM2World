package org.osm2world.core.world.modules;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openstreetmap.josm.plugins.graphview.core.data.EmptyTagGroup;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWay;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.world.modules.RoadModule.Road;

import com.slimjars.dist.gnu.trove.list.TLongList;
import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;

import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.core.model.impl.Way;

public class FindClosestJunctionTest {

	@Test
	public void zeroConnectedJunctions() throws Exception {

		/* create fake data */

		List<Node> nodes = new ArrayList<Node>();
		nodes.add(new Node(100, 0, 0));
		nodes.add(new Node(101, 0.5, 0));
		nodes.add(new Node(102, 1.0, 0));
		nodes.add(new Node(103, 1.5, 0));

		TLongList nodeIds = new TLongArrayList(new long[] {100, 101, 102, 103});

		MapNode node0 = new MapNode(new VectorXZ(0,0), nodes.get(0));
		MapNode node1 = new MapNode(new VectorXZ(1,1), nodes.get(1));
		MapNode node2 = new MapNode(new VectorXZ(2,2), nodes.get(2));
		MapNode node3 = new MapNode(new VectorXZ(3,3), nodes.get(3));

		Way way = new Way(201, nodeIds);
		way.setTags(asList(new Tag("highway", "tertiary")));

		/* create way with 4 nodes (3 segments) */

		MapWay mapWay = new MapWay(way, asList(node0, node1, node2, node3));

		/* get a reflection of MapWaySegment's constructor */

		Constructor<MapWaySegment> c = MapWaySegment.class.getDeclaredConstructor(new Class[] {MapWay.class, MapNode.class, MapNode.class});
		c.setAccessible(true);
		MapWaySegment segment0 = c.newInstance(mapWay, node0, node1);
		MapWaySegment segment1 = c.newInstance(mapWay, node1, node2);
		MapWaySegment segment2 = c.newInstance(mapWay, node2, node3);

		Road road0 = new RoadModule.Road(segment0, EmptyTagGroup.EMPTY_TAG_GROUP);
		segment0.addRepresentation(road0);

		Road road1 = new RoadModule.Road(segment1, EmptyTagGroup.EMPTY_TAG_GROUP);
		segment1.addRepresentation(road1);

		Road road2 = new RoadModule.Road(segment2, EmptyTagGroup.EMPTY_TAG_GROUP);
		segment2.addRepresentation(road2);

		/* add segments to the nodes */
		node0.addOutboundLine(segment0);

		node1.addInboundLine(segment0);
		node1.addOutboundLine(segment1);

		node2.addInboundLine(segment1);
		node2.addOutboundLine(segment2);

		node3.addInboundLine(segment2);

		//give a random value to the node we will use to verify the junction result
		MapNode nn = new MapNode(new VectorXZ(5,5), new Node(1111, 0, 0));

		if(RoadModule.getConnectedRoads(node1, false).size()<=2 && RoadModule.getConnectedRoads(node1, false).size()>0) {

			nn = TrafficSignModule.findClosestJunction(node1);
		}

		assertTrue("intersection node: "+nn+" should have been null", nn==null);
	}

	@Test
	public void oneConnectedJunction() throws Exception {

		/* create fake data */

		List<Node> nodes = new ArrayList<Node>();
		nodes.add(new Node(100, 0, 0));
		nodes.add(new Node(101, 0.5, 0));
		nodes.add(new Node(102, 1.0, 0));
		nodes.add(new Node(103, 1.5, 0));

		List<Node> junctionNodes1 = new ArrayList<Node>();
		junctionNodes1.add(new Node(300, 2, 0));

		List<Node> junctionNodes2 = new ArrayList<Node>();
		junctionNodes2.add(new Node(301, 2.5, 0));

		TLongList nodeIds = new TLongArrayList(new long[] {100, 101, 102, 103});

		TLongList junctionNodes1Ids = new TLongArrayList(new long[] {103,300});

		TLongList junctionNodes2Ids = new TLongArrayList(new long[] {103,301});

		MapNode node0 = new MapNode(new VectorXZ(0,0), nodes.get(0));
		MapNode node1 = new MapNode(new VectorXZ(1,1), nodes.get(1));
		MapNode node2 = new MapNode(new VectorXZ(2,2), nodes.get(2));
		MapNode node3 = new MapNode(new VectorXZ(3,3), nodes.get(3));

		MapNode node4 = new MapNode(new VectorXZ(3,1), junctionNodes1.get(0));

		MapNode node5 = new MapNode(new VectorXZ(4,3), junctionNodes2.get(0));


		Way way = new Way(201, nodeIds);
		way.setTags(asList(new Tag("highway", "tertiary")));

		Way way2 = new Way(202, junctionNodes1Ids);
		way2.setTags(asList(new Tag("highway", "tertiary")));

		Way way3 = new Way(203, junctionNodes2Ids);
		way3.setTags(asList(new Tag("highway", "tertiary")));

		/* create ways */

		MapWay mapWay = new MapWay(way, asList(node0, node1, node2, node3));
		MapWay mapWay2 = new MapWay(way2, asList(node3, node4));
		MapWay mapWay3 = new MapWay(way3, asList(node3, node5));

		/* get a reflection of MapWaySegment's constructor */

		Constructor<MapWaySegment> c = MapWaySegment.class.getDeclaredConstructor(new Class[] {MapWay.class, MapNode.class, MapNode.class});
		c.setAccessible(true);
		MapWaySegment segment0 = c.newInstance(mapWay, node0, node1);
		MapWaySegment segment1 = c.newInstance(mapWay, node1, node2);
		MapWaySegment segment2 = c.newInstance(mapWay, node2, node3);

		MapWaySegment segment3 = c.newInstance(mapWay2, node3, node4);

		MapWaySegment segment4 = c.newInstance(mapWay3, node3, node5);

		Road road0 = new RoadModule.Road(segment0, EmptyTagGroup.EMPTY_TAG_GROUP);
		segment0.addRepresentation(road0);

		Road road1 = new RoadModule.Road(segment1, EmptyTagGroup.EMPTY_TAG_GROUP);
		segment1.addRepresentation(road1);

		Road road2 = new RoadModule.Road(segment2, EmptyTagGroup.EMPTY_TAG_GROUP);
		segment2.addRepresentation(road2);

		Road road3 = new RoadModule.Road(segment3, EmptyTagGroup.EMPTY_TAG_GROUP);
		segment3.addRepresentation(road3);

		Road road4 = new RoadModule.Road(segment4, EmptyTagGroup.EMPTY_TAG_GROUP);
		segment4.addRepresentation(road4);

		/* add segments to the nodes */
		node0.addOutboundLine(segment0);

		node1.addInboundLine(segment0);
		node1.addOutboundLine(segment1);

		node2.addInboundLine(segment1);
		node2.addOutboundLine(segment2);

		node3.addInboundLine(segment2);
		node3.addOutboundLine(segment3);
		node3.addOutboundLine(segment4);

		//give a random value to the node we will use to verify the junction result
		MapNode nn = new MapNode(new VectorXZ(5,5), new Node(1111, 0, 0));

		if(RoadModule.getConnectedRoads(node1, false).size()<=2 && RoadModule.getConnectedRoads(node1, false).size()>0) {

			nn = TrafficSignModule.findClosestJunction(node1);
		}

		assertTrue("intersection node: "+nn+" should have been n103", nn.equals(node3));
	}

	@Test
	public void twoConnectedJunctions() throws Exception {

		/* create fake data */

		List<Node> nodes = new ArrayList<Node>();
		nodes.add(new Node(100, 0, 0));
		nodes.add(new Node(101, 0.5, 0));
		nodes.add(new Node(102, 1.0, 0));
		nodes.add(new Node(103, 1.5, 0));

		List<Node> junctionNodes1 = new ArrayList<Node>();
		junctionNodes1.add(new Node(300, 2, 0));

		List<Node> junctionNodes2 = new ArrayList<Node>();
		junctionNodes2.add(new Node(301, 2.5, 0));

		List<Node> junctionNodes3 = new ArrayList<Node>();
		junctionNodes3.add(new Node(401, 3.5, 0));

		List<Node> junctionNodes4 = new ArrayList<Node>();
		junctionNodes4.add(new Node(501, 4.5, 0));

		TLongList nodeIds = new TLongArrayList(new long[] {100, 101, 102, 103});

		TLongList junctionNodes1Ids = new TLongArrayList(new long[] {103,300});

		TLongList junctionNodes2Ids = new TLongArrayList(new long[] {103,301});

		TLongList junctionNodes3Ids = new TLongArrayList(new long[] {100,401});

		TLongList junctionNodes4Ids = new TLongArrayList(new long[] {100,501});

		MapNode node0 = new MapNode(new VectorXZ(0,0), nodes.get(0));
		MapNode node1 = new MapNode(new VectorXZ(1,1), nodes.get(1));
		MapNode node2 = new MapNode(new VectorXZ(2,2), nodes.get(2));
		MapNode node3 = new MapNode(new VectorXZ(3,3), nodes.get(3));

		MapNode node4 = new MapNode(new VectorXZ(3,1), junctionNodes1.get(0));

		MapNode node5 = new MapNode(new VectorXZ(4,3), junctionNodes2.get(0));

		MapNode node6 = new MapNode(new VectorXZ(0,1), junctionNodes3.get(0));

		MapNode node7 = new MapNode(new VectorXZ(-1,0), junctionNodes4.get(0));


		Way way = new Way(201, nodeIds);
		way.setTags(asList(new Tag("highway", "tertiary")));

		Way way2 = new Way(202, junctionNodes1Ids);
		way2.setTags(asList(new Tag("highway", "tertiary")));

		Way way3 = new Way(203, junctionNodes2Ids);
		way3.setTags(asList(new Tag("highway", "tertiary")));

		Way way4 = new Way(204, junctionNodes3Ids);
		way4.setTags(asList(new Tag("highway", "tertiary")));

		Way way5 = new Way(205, junctionNodes4Ids);
		way5.setTags(asList(new Tag("highway", "tertiary")));

		/* create ways */

		MapWay mapWay = new MapWay(way, asList(node0, node1, node2, node3));
		MapWay mapWay2 = new MapWay(way2, asList(node3, node4));
		MapWay mapWay3 = new MapWay(way3, asList(node3, node5));
		MapWay mapWay4 = new MapWay(way4, asList(node0, node6));
		MapWay mapWay5 = new MapWay(way5, asList(node0, node7));

		/* get a reflection of MapWaySegment's constructor */

		Constructor<MapWaySegment> c = MapWaySegment.class.getDeclaredConstructor(new Class[] {MapWay.class, MapNode.class, MapNode.class});
		c.setAccessible(true);
		MapWaySegment segment0 = c.newInstance(mapWay, node0, node1);
		MapWaySegment segment1 = c.newInstance(mapWay, node1, node2);
		MapWaySegment segment2 = c.newInstance(mapWay, node2, node3);

		MapWaySegment segment3 = c.newInstance(mapWay2, node3, node4);

		MapWaySegment segment4 = c.newInstance(mapWay3, node3, node5);

		MapWaySegment segment5 = c.newInstance(mapWay4, node0, node6);

		MapWaySegment segment6 = c.newInstance(mapWay5, node0, node7);

		Road road0 = new RoadModule.Road(segment0, EmptyTagGroup.EMPTY_TAG_GROUP);
		segment0.addRepresentation(road0);

		Road road1 = new RoadModule.Road(segment1, EmptyTagGroup.EMPTY_TAG_GROUP);
		segment1.addRepresentation(road1);

		Road road2 = new RoadModule.Road(segment2, EmptyTagGroup.EMPTY_TAG_GROUP);
		segment2.addRepresentation(road2);

		Road road3 = new RoadModule.Road(segment3, EmptyTagGroup.EMPTY_TAG_GROUP);
		segment3.addRepresentation(road3);

		Road road4 = new RoadModule.Road(segment4, EmptyTagGroup.EMPTY_TAG_GROUP);
		segment4.addRepresentation(road4);

		Road road5 = new RoadModule.Road(segment5, EmptyTagGroup.EMPTY_TAG_GROUP);
		segment5.addRepresentation(road5);

		Road road6 = new RoadModule.Road(segment6, EmptyTagGroup.EMPTY_TAG_GROUP);
		segment6.addRepresentation(road6);

		/* add segments to the nodes */
		node0.addOutboundLine(segment0);
		node0.addOutboundLine(segment5);
		node0.addOutboundLine(segment6);

		node1.addInboundLine(segment0);
		node1.addOutboundLine(segment1);

		node2.addInboundLine(segment1);
		node2.addOutboundLine(segment2);

		node3.addInboundLine(segment2);
		node3.addOutboundLine(segment3);
		node3.addOutboundLine(segment4);

		//give a random value to the node we will use to verify the junction result
		MapNode nn = new MapNode(new VectorXZ(5,5), new Node(1111, 0, 0));

		if(RoadModule.getConnectedRoads(node1, false).size()<=2 && RoadModule.getConnectedRoads(node1, false).size()>0) {

			nn = TrafficSignModule.findClosestJunction(node1);
		}

		/* The closest of the 2 junctions is the one of node0 */

		assertTrue("intersection node: "+nn+" should have been n100", nn.equals(node0));
	}

}
