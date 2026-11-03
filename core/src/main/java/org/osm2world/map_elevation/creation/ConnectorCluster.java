package org.osm2world.map_elevation.creation;

import static java.lang.Math.abs;

import java.util.*;

import javax.annotation.Nullable;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.map_data.data.MapElement;
import org.osm2world.map_data.data.MapRelationElement;
import org.osm2world.math.VectorXZ;
import org.osm2world.util.FaultTolerantIterationUtil;
import org.osm2world.world.connectors.*;
import org.osm2world.world.data.WorldObject;

/**
 * a group of {@link Connector}s whose elevations are fixed relative to each other,
 * so that they can only move up or down together.
 * Each cluster has a single free value, its base elevation, which is the elevation of its lowest member.
 */
public final class ConnectorCluster {

	/** if candidate base elevations within a cluster differ by more than this, a warning is logged */
	private static final double WARNING_THRESHOLD = 1.0;

	private final List<Connector> connectors;

	private double baseEle;

	/** each connector's elevation minus the base elevation (>= 0), in the same order as {@link #connectors} */
	private final double[] eleOffsets;

	/** {@link EleRelation}s which determine the elevation of one of this cluster's connectors */
	private final List<LowestGroundContact> groundContacts;

	private ConnectorCluster(List<Connector> connectors, double[] eleOffsets, List<LowestGroundContact> groundContacts,
			double baseEle) {
		assert connectors.size() == eleOffsets.length;
		this.connectors = connectors;
		this.eleOffsets = eleOffsets;
		this.groundContacts = groundContacts;
		this.baseEle = baseEle;
	}

	/**
	 * collects the connectors and relations of the given objects and groups the connectors into clusters.
	 * Every connector is part of exactly one cluster.
	 * <p>
	 * The grouping is based on {@link Connector#connectsTo(Connector)} and {@link RigidConnectorSet}s.
	 * <p>
	 * Creating clusters does not move any connectors. Connectors are only moved by {@link #setBaseEle(double)}.
	 */
	public static List<ConnectorCluster> fromWorldObjects(Iterable<WorldObject> worldObjects) {

		/* collect connectors and relations */

		List<Connector> connectors = new ArrayList<>();
		Map<Connector, WorldObject> objectForConnector = new IdentityHashMap<>();
		List<RigidConnectorSet> rigidSets = new ArrayList<>();
		Map<RigidConnectorSet, WorldObject> objectForRigidSet = new IdentityHashMap<>();
		List<LowestGroundContact> groundContacts = new ArrayList<>();

		FaultTolerantIterationUtil.forEach(worldObjects, (WorldObject worldObject) -> {
			for (Connector c : worldObject.getConnectors()) {
				if (objectForConnector.putIfAbsent(c, worldObject) == null) {
					connectors.add(c);
				}
			}
			for (EleRelation relation : worldObject.getEleRelations()) {
				if (relation instanceof RigidConnectorSet set) {
					rigidSets.add(set);
					objectForRigidSet.put(set, worldObject);
				} else if (relation instanceof LowestGroundContact groundContact) {
					groundContacts.add(groundContact);
					if (objectForConnector.putIfAbsent(groundContact.connector(), worldObject) == null) {
						connectors.add(groundContact.connector());
					}
				}
			}
		});

		var unionFind = new UnionFind();
		connectors.forEach(unionFind::add);

		/* apply rigid sets */

		Set<Connector> connectorsInRigidSets = Collections.newSetFromMap(new IdentityHashMap<>());

		for (RigidConnectorSet set : rigidSets) {

			WorldObject object = objectForRigidSet.get(set);
			Connector first = set.getConnectors().iterator().next();

			for (Connector c : set.getConnectors()) {

				if (objectForConnector.putIfAbsent(c, object) == null) {
					connectors.add(c);
				}

				if (!connectorsInRigidSets.add(c)) {
					ConversionLog.warn("Connector is part of multiple rigid sets", object);
				}

				if (!unionFind.relate(c, first, set.getOffset(c) - set.getOffset(first))) {
					ConversionLog.warn("Contradictory rigid connector sets, ignoring part of " + set, object);
				}

			}

		}

		/* apply connections between connectors at the same position */

		Map<VectorXZ, List<Connector>> connectorsByPos = new HashMap<>();
		for (Connector c : connectors) {
			connectorsByPos.computeIfAbsent(c.pos, k -> new ArrayList<>()).add(c);
		}

		for (List<Connector> connectorsAtPos : connectorsByPos.values()) {
			for (int i = 0; i < connectorsAtPos.size(); i++) {
				for (int j = i + 1; j < connectorsAtPos.size(); j++) {
					Connector a = connectorsAtPos.get(i);
					Connector b = connectorsAtPos.get(j);
					if (a.connectsTo(b) && !unionFind.relate(a, b, 0)) {
						ConversionLog.warn("Ignoring connection which contradicts other elevation relations",
								objectForConnector.get(a));
					}
				}
			}
		}

		/* determine initial elevations: terrain elevation, adjusted for rigid sets */

		Map<Connector, Double> initialEleForConnector = new IdentityHashMap<>();

		for (Connector c : connectors) {
			initialEleForConnector.put(c, getTerrainEle(c));
		}

		for (RigidConnectorSet set : rigidSets) {
			double baseEle = getInitialBaseEle(set);
			for (Connector c : set.getConnectors()) {
				initialEleForConnector.put(c, baseEle + set.getOffset(c));
			}
		}

		/* create a cluster for each root of the union-find structure */

		Map<Connector, List<Connector>> membersByRoot = new LinkedHashMap<>();
		for (Connector c : connectors) {
			membersByRoot.computeIfAbsent(unionFind.getRoot(c), k -> new ArrayList<>()).add(c);
		}

		Map<Connector, List<LowestGroundContact>> groundContactsByRoot = new IdentityHashMap<>();
		for (LowestGroundContact groundContact : groundContacts) {
			groundContactsByRoot.computeIfAbsent(unionFind.getRoot(groundContact.connector()),
					k -> new ArrayList<>()).add(groundContact);
		}

		List<ConnectorCluster> result = new ArrayList<>(membersByRoot.size());

		for (var entry : membersByRoot.entrySet()) {

			List<Connector> members = entry.getValue();

			// offsets relative to the lowest member (rather than the union-find root, which is arbitrary)
			double[] offsets = new double[members.size()];
			for (int i = 0; i < members.size(); i++) {
				offsets[i] = unionFind.getOffsetToRoot(members.get(i));
			}
			double minOffset = Arrays.stream(offsets).min().orElseThrow();
			for (int i = 0; i < offsets.length; i++) {
				offsets[i] -= minOffset;
			}

			/* use the highest base elevation implied by members on the ground (or by any member, if none are) */

			List<Connector> votingMembers = selectVotingMembers(members);

			double minBaseEle = Double.POSITIVE_INFINITY;
			double maxBaseEle = Double.NEGATIVE_INFINITY;

			for (int i = 0; i < members.size(); i++) {
				Connector c = members.get(i);
				if (votingMembers.contains(c)) {
					double value = initialEleForConnector.get(c) - offsets[i];
					minBaseEle = Math.min(minBaseEle, value);
					maxBaseEle = Math.max(maxBaseEle, value);
				}
			}

			var cluster = new ConnectorCluster(List.copyOf(members), offsets,
					List.copyOf(groundContactsByRoot.getOrDefault(entry.getKey(), List.of())), maxBaseEle);

			if (maxBaseEle - minBaseEle > WARNING_THRESHOLD) {
				ConversionLog.warn("Connected objects suggest different elevations", cluster.getElementForLogging());
			}

			result.add(cluster);

		}

		return result;

	}

	/**
	 * returns the members whose terrain elevation is used for the initial base elevation:
	 * Members on the ground if there are any, otherwise all members which are not {@link GroundState#DERIVED},
	 * or all members as a last resort.
	 */
	private static List<Connector> selectVotingMembers(Collection<Connector> members) {
		List<Connector> result = members.stream().filter(c -> c.groundState == GroundState.ON).toList();
		if (result.isEmpty()) {
			result = members.stream().filter(c -> c.groundState != GroundState.DERIVED).toList();
		}
		if (result.isEmpty()) {
			result = List.copyOf(members);
		}
		return result;
	}

	/** returns the connectors in this cluster */
	public List<Connector> getConnectors() {
		return connectors;
	}

	/** returns the {@link LowestGroundContact} relations for connectors in this cluster */
	public List<LowestGroundContact> getGroundContacts() {
		return groundContacts;
	}

	/**
	 * returns the connector's elevation minus the cluster's base elevation.
	 * This searches the cluster's connectors, so it is intended for clusters with few members.
	 */
	public double getOffset(Connector connector) {
		for (int i = 0; i < connectors.size(); i++) {
			if (connectors.get(i) == connector) {
				return eleOffsets[i];
			}
		}
		throw new IllegalArgumentException("connector is not part of this cluster: " + connector);
	}

	/**
	 * returns a map element associated with this cluster, for use in log messages.
	 * Based on the members' {@link Connector#reference} or {@link Connector#object}.
	 *
	 * @return  a map element, or null if none is known
	 */
	public @Nullable MapRelationElement getElementForLogging() {
		for (Connector c : connectors) {
			if (c.reference instanceof MapRelationElement element) {
				return element;
			} else if (c.reference instanceof MapElement element) {
				return element.getElementWithId();
			} else if (c.object != null && c.object.getPrimaryMapElement() != null) {
				return c.object.getPrimaryMapElement().getElementWithId();
			}
		}
		return null;
	}

	/** returns the base elevation implied by an attached member, or null if no member is attached */
	public @Nullable Double getBaseEleFromAttachment() {
		for (int i = 0; i < connectors.size(); i++) {
			if (connectors.get(i).isAttached()) {
				return connectors.get(i).getPosXYZ().y - eleOffsets[i];
			}
		}
		return null;
	}

	/**
	 * returns the cluster's base elevation, i.e. the elevation of its lowest member(s).
	 * Each member's elevation is this value plus the member's offset.
	 */
	public double getBaseEle() {
		return baseEle;
	}

	/**
	 * sets the cluster's base elevation and moves all members which are not attached accordingly.
	 * Attached members keep the position the attachment gave them.
	 */
	public void setBaseEle(double baseEle) {
		this.baseEle = baseEle;
		for (int i = 0; i < connectors.size(); i++) {
			Connector c = connectors.get(i);
			if (!c.isAttached()) {
				c.setPosXYZ(c.pos.xyz(baseEle + eleOffsets[i]));
			}
		}
	}

	@Override
	public String toString() {
		return "ConnectorCluster" + connectors;
	}

	/** returns the connector's terrain elevation, or its current elevation if no terrain elevation has been set */
	private static double getTerrainEle(Connector c) {
		return Double.isNaN(c.getTerrainEle()) ? c.getPosXYZ().y : c.getTerrainEle();
	}

	/**
	 * derives a rigid set's base elevation from the terrain elevation at its members, using the set's
	 * {@link RigidConnectorSet.GroundEleRule}. Uses the members selected by {@link #selectVotingMembers(Collection)}.
	 */
	private static double getInitialBaseEle(RigidConnectorSet set) {

		List<Connector> members = selectVotingMembers(set.getConnectors());

		var values = members.stream().mapToDouble(c -> getTerrainEle(c) - set.getOffset(c));

		return switch (set.getGroundEleRule()) {
			case LOWEST -> values.min().orElseThrow();
			case HIGHEST -> values.max().orElseThrow();
		};

	}

	/**
	 * union-find structure which also tracks each connector's elevation offset relative to its parent.
	 * Used while building the clusters.
	 */
	private static final class UnionFind {

		private static final double TOLERANCE = 1e-6;

		private final Map<Connector, Connector> parent = new IdentityHashMap<>();

		/** for each connector: its elevation minus its parent's elevation */
		private final Map<Connector, Double> offsetToParent = new IdentityHashMap<>();

		/** for each root: the number of connectors in its cluster */
		private final Map<Connector, Integer> size = new IdentityHashMap<>();

		/** adds a connector as a cluster of its own, unless it has already been added */
		void add(Connector connector) {
			if (!parent.containsKey(connector)) {
				parent.put(connector, connector);
				offsetToParent.put(connector, 0.0);
				size.put(connector, 1);
			}
		}

		/**
		 * requires the elevation of parameter "a" to be the elevation of parameter "b" plus the given offset.
		 * Merges the clusters of a and b if necessary.
		 *
		 * @return false if this contradicts the existing relations. In that case, nothing is changed.
		 */
		boolean relate(Connector a, Connector b, double offset) {

			add(a);
			add(b);

			Connector rootA = getRoot(a);
			Connector rootB = getRoot(b);
			double offsetA = offsetToParent.get(a);
			double offsetB = offsetToParent.get(b);

			if (rootA == rootB) {
				return abs(offsetA - offsetB - offset) < TOLERANCE;
			}

			// ele(rootA) = ele(a) - offsetA = ele(b) + offset - offsetA = ele(rootB) + offsetB + offset - offsetA
			double rootOffset = offsetB + offset - offsetA;

			if (size.get(rootA) <= size.get(rootB)) {
				parent.put(rootA, rootB);
				offsetToParent.put(rootA, rootOffset);
				size.put(rootB, size.get(rootB) + size.remove(rootA));
			} else {
				parent.put(rootB, rootA);
				offsetToParent.put(rootB, -rootOffset);
				size.put(rootA, size.get(rootA) + size.remove(rootB));
			}

			return true;

		}

		/** returns the root connector of the cluster which contains the connector */
		Connector getRoot(Connector connector) {

			List<Connector> path = new ArrayList<>();
			Connector current = connector;
			while (parent.get(current) != current) {
				path.add(current);
				current = parent.get(current);
			}
			Connector root = current;

			// path compression, starting with the connector closest to the root
			for (int i = path.size() - 1; i >= 0; i--) {
				Connector c = path.get(i);
				Connector p = parent.get(c);
				if (p != root) {
					offsetToParent.put(c, offsetToParent.get(c) + offsetToParent.get(p));
					parent.put(c, root);
				}
			}

			return root;

		}

		/** returns the connector's elevation minus the elevation of its cluster's root */
		double getOffsetToRoot(Connector connector) {
			getRoot(connector);
			return offsetToParent.get(connector);
		}

	}

}
