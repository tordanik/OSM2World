package org.osm2world.map_elevation.creation;

import static org.junit.Assert.*;
import static org.osm2world.world.connectors.RigidConnectorSet.GroundEleRule.LOWEST;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.osm2world.map_data.data.MapElement;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.math.VectorXZ;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.world.connectors.Connector;
import org.osm2world.world.connectors.EleRelation;
import org.osm2world.world.connectors.GroundState;
import org.osm2world.world.connectors.RigidConnectorSet;
import org.osm2world.world.data.WorldObject;

public class ConnectorClusterTest {

	private static MapNode node(double x) {
		return new MapNode(0, TagSet.of(), new VectorXZ(x, 0));
	}

	/** creates a connector which joins other connectors referencing the same node */
	private static Connector connector(MapNode node) {
		var c = new Connector(node.getPos(), node, GroundState.ABOVE);
		c.setTerrainEle(0);
		return c;
	}

	/** creates a connector which doesn't join any others */
	private static Connector connector(double x) {
		var c = new Connector(new VectorXZ(x, 0), null, GroundState.ABOVE);
		c.setTerrainEle(0);
		return c;
	}

	private static WorldObject testObject(List<Connector> connectors, List<EleRelation> relations) {
		return new WorldObject() {
			@Override public List<Mesh> buildMeshes() { return new ArrayList<>(); }
			@Override public MapElement getPrimaryMapElement() { return null; }
			@Override public Iterable<Connector> getConnectors() { return connectors; }
			@Override public Collection<EleRelation> getEleRelations() { return relations; }
		};
	}

	/** returns the cluster containing a connector */
	private static ConnectorCluster clusterOf(List<ConnectorCluster> clusters, Connector c) {
		return clusters.stream().filter(it -> it.getConnectors().contains(c)).findFirst().orElseThrow();
	}

	/** returns ele(a) - ele(b), which is only defined if both are in the same cluster */
	private static double relativeEle(List<ConnectorCluster> clusters, Connector a, Connector b) {
		ConnectorCluster cluster = clusterOf(clusters, a);
		assertSame(cluster, clusterOf(clusters, b));
		return cluster.getOffset(a) - cluster.getOffset(b);
	}

	@Test
	public void testChainedOffsets() {

		MapNode n1 = node(1), n2 = node(2);

		// three rigid sets, chained together by joins at n1 and n2
		Connector a = connector(0), b = connector(n1);
		Connector c = connector(n1), d = connector(n2);
		Connector e = connector(n2), f = connector(3);

		var clusters = ConnectorCluster.fromWorldObjects(List.of(
				testObject(List.of(a, b), List.of(new RigidConnectorSet(LOWEST, Map.of(a, 0.0, b, 1.0)))),
				testObject(List.of(c, d), List.of(new RigidConnectorSet(LOWEST, Map.of(c, 0.0, d, 2.0)))),
				testObject(List.of(e, f), List.of(new RigidConnectorSet(LOWEST, Map.of(e, 0.0, f, -5.0))))));

		assertEquals(1, clusters.size());
		assertEquals(1, relativeEle(clusters, b, a), 1e-9);
		assertEquals(1, relativeEle(clusters, c, a), 1e-9);
		assertEquals(3, relativeEle(clusters, d, a), 1e-9);
		assertEquals(3, relativeEle(clusters, e, a), 1e-9);
		assertEquals(-2, relativeEle(clusters, f, a), 1e-9);

	}

	@Test
	public void testContradictionIsIgnored() {

		MapNode n1 = node(0), n2 = node(10);

		Connector a = connector(n1), b = connector(n2);
		Connector c = connector(n1), d = connector(n2);

		var clusters = ConnectorCluster.fromWorldObjects(List.of(
				testObject(List.of(a, b), List.of(new RigidConnectorSet(LOWEST, Map.of(a, 0.0, b, 3.0)))),
				testObject(List.of(c, d), List.of(new RigidConnectorSet(LOWEST, Map.of(c, 0.0, d, 0.0))))));

		// one of the two joins contradicts the rigid sets and is ignored, the sets remain intact
		assertEquals(1, clusters.size());
		assertEquals(3, relativeEle(clusters, b, a), 1e-9);
		assertEquals(0, relativeEle(clusters, d, c), 1e-9);

	}

	@Test
	public void testSeparateClusters() {

		MapNode n = node(5);
		Connector a = connector(n), b = connector(7), c = connector(n);

		var clusters = ConnectorCluster.fromWorldObjects(List.of(testObject(List.of(a, b, c), List.of())));

		assertEquals(List.of(List.of(a, c), List.of(b)),
				clusters.stream().map(ConnectorCluster::getConnectors).toList());

	}

	@Test
	public void testRigidSetAndJoin() {

		MapNode n = node(5);

		Connector a = new Connector(VectorXZ.NULL_VECTOR, null, GroundState.ON);
		a.setTerrainEle(0);
		Connector b = connector(n);
		Connector c = connector(n);
		Connector d = connector(9);

		var set = new RigidConnectorSet(LOWEST, Map.of(a, 0.0, b, 3.0));

		WorldObject object1 = testObject(List.of(a, b), List.of(set));
		WorldObject object2 = testObject(List.of(c, d), List.of());

		var clusters = ConnectorCluster.fromWorldObjects(List.of(object1, object2));

		assertEquals(2, clusters.size());
		assertEquals(3, relativeEle(clusters, c, a), 1e-9);
		assertNotSame(clusterOf(clusters, a), clusterOf(clusters, d));

	}

	@Test
	public void testInitialBaseEle() {

		for (var rule : RigidConnectorSet.GroundEleRule.values()) {

			MapNode n = node(5);

			// a rigid set with two connectors on the ground and one above it
			Connector a = new Connector(new VectorXZ(0, 0), null, GroundState.ON);
			Connector b = new Connector(new VectorXZ(10, 0), null, GroundState.ON);
			Connector entrance = connector(n);
			var set = new RigidConnectorSet(rule, Map.of(a, 0.0, b, 0.0, entrance, 3.0));

			// a connector which isn't part of a rigid set, joined to the entrance
			Connector other = connector(n);

			a.setTerrainEle(2);
			b.setTerrainEle(4);
			entrance.setTerrainEle(10);
			other.setTerrainEle(7);

			var clusters = ConnectorCluster.fromWorldObjects(List.of(
					testObject(List.of(a, b, entrance), List.of(set)),
					testObject(List.of(other), List.of())));

			// only the members on the ground determine the initial base elevation, using the set's rule
			ConnectorCluster cluster = clusterOf(clusters, a);
			double expectedBase = rule == LOWEST ? 2 : 4;
			assertEquals(expectedBase, cluster.getBaseEle(), 1e-9);

			// setting the base elevation moves all members according to their offsets
			cluster.setBaseEle(20);
			assertEquals(20, a.getPosXYZ().y, 1e-9);
			assertEquals(20, b.getPosXYZ().y, 1e-9);
			assertEquals(23, entrance.getPosXYZ().y, 1e-9);
			assertEquals(23, other.getPosXYZ().y, 1e-9);

		}

	}

	@Test
	public void testInitialEleWithoutGroundMembers() {

		Connector a = connector(0), b = connector(10);
		a.setTerrainEle(2);
		b.setTerrainEle(5);

		var clusters = ConnectorCluster.fromWorldObjects(List.of(
				testObject(List.of(a, b), List.of(new RigidConnectorSet(LOWEST, Map.of(a, 0.0, b, 1.0))))));

		// no members on the ground, so all members are used (with the set's rule: min(2 - 0, 5 - 1))
		assertEquals(2, clusters.get(0).getBaseEle(), 1e-9);

	}

	@Test(expected = IllegalArgumentException.class)
	public void testUnknownConnector() {
		var clusters = ConnectorCluster.fromWorldObjects(List.of(testObject(List.of(connector(0)), List.of())));
		clusters.get(0).getOffset(connector(1));
	}

}
