package org.osm2world.core.map_elevation.creation;

import static java.util.Arrays.asList;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.batik.bridge.Bridge;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.modules.BridgeModule;
import org.osm2world.core.world.modules.RoadModule;
import org.osm2world.core.world.modules.RoadModule.Road;
import org.osm2world.core.world.modules.RoadModule.RoadConnector;

/**
 * enforcer implementation that ignores many of the constraints, but is much
 * faster than the typical full implementation.
 *
 * It tries to produce an output that is "good enough" for some purposes, and is
 * therefore a compromise between the {@link NoneEleConstraintEnforcer} and a
 * full implementation.
 */
// implemented by spinachpasta
// This enforcer configures elavation using Diffusion equation (Heat equation)
public final class DiffusionEleConstraintEnforcer implements EleConstraintEnforcer {

	private Collection<EleConnector> connectors = new ArrayList<EleConnector>();

	/**
	 * associates each EleConnector with the {@link StiffConnectorSet} it is part of
	 * (if any)
	 */
	private Map<EleConnector, StiffConnectorSet> stiffSetMap = new HashMap<EleConnector, StiffConnectorSet>();

	int connectedCount = 0;

	class RoadNetworkEdge {
		public EleConnector a;
		public EleConnector b;
		public double distance;

		public static final double dx = 2f;

		public List<Double> h = new ArrayList<Double>();
		public List<Double> h1 = new ArrayList<Double>();

		public RoadNetworkEdge(EleConnector start, EleConnector end, double d) {
			a = start;
			b = end;
			distance = d;
			for (int i = 0; i < /* distance / dx + 2 */2; i++) {
				h.add(0.0);
				h1.add(0.0);
			}
		}

		// calculate diffusion
		public void step(double dt, double conductance) {
			{
				int i = 0;
				double dhdt = conductance * (h.get(i + 1) - h.get(i)) / dx;
				h1.set(i, h.get(i) + dhdt * dt);
			}
			{
				int i = h.size() - 1;
				double dhdt = conductance * (h.get(i - 1) - h.get(i)) / dx;
				h1.set(i, h.get(i) + dhdt * dt);
			}
			for (int i = 1; i < h.size() - 1; i++) {
				double dhdt = conductance * (h.get(i + 1) - 2 * h.get(i) + h.get(i - 1)) / dx;
				h1.set(i, h.get(i) + dhdt * dt);
			}
			for (int i = 0; i < h.size(); i++) {
				h.set(i, h1.get(i));
			}
			if(h.get(0)!=h.get(1)){
				System.out.println(h);
			}
		}

		public void setStartEnd(double start, double end) {
			h.set(0, start);
			h.set(h.size() - 1, end);
		}

		public double getStart() {
			return h.get(0);
		}

		public double getEnd() {
			return h.get(h.size() - 1);
		}

		public double getHeight(double pos/* between 0 and 1 */) {
			int index = (int) Math.floor(pos * (h.size() - 1));
			if (index >= h.size() - 1) {
				return getEnd();
			}
			double interpolation = pos * (h.size() - 1.0) - index;
			return h.get(index) * interpolation + h.get(index + 1) * (1 - interpolation);
		}
	}

	Map<EleConnector, List<RoadNetworkEdge>> roadGraph = new HashMap<EleConnector, List<RoadNetworkEdge>>();;

	@Override
	public void addConnectors(Iterable<EleConnector> newConnectors) {

		for (EleConnector c : newConnectors) {
			connectors.add(c);
		}

		/* connect connectors */

		for (EleConnector c1 : newConnectors) {
			for (EleConnector c2 : connectors) {

				if (c1 != c2 && c1.connectsTo(c2)) {
					requireSameEle(c1, c2);
				}

			}
		}

	}

	@Override
	public void requireSameEle(EleConnector c1, EleConnector c2) {

		// SUGGEST (performance): a special case implementation would be faster

		requireSameEle(asList(c1, c2));

	}

	@Override
	public void requireSameEle(Iterable<EleConnector> cs) {

		/* find stiff sets containing any of the affected connectors */

		Set<EleConnector> looseConnectors = new HashSet<EleConnector>();
		Set<StiffConnectorSet> existingStiffSets = new HashSet<StiffConnectorSet>();

		for (EleConnector c : cs) {

			StiffConnectorSet stiffSet = stiffSetMap.get(c);

			if (stiffSet != null) {
				existingStiffSets.add(stiffSet);
			} else {
				looseConnectors.add(c);
			}

		}

		/* return if the connectors are already in a set together */

		if (existingStiffSets.size() == 1 && looseConnectors.isEmpty())
			return;

		/* merge existing sets (if any) into a single set */

		StiffConnectorSet commonStiffSet = null;

		if (existingStiffSets.isEmpty()) {
			commonStiffSet = new StiffConnectorSet();
		} else {

			for (StiffConnectorSet stiffSet : existingStiffSets) {

				if (commonStiffSet == null) {
					commonStiffSet = stiffSet;
				} else {

					for (EleConnector c : stiffSet) {
						stiffSetMap.put(c, commonStiffSet);
					}

					commonStiffSet.mergeFrom(stiffSet);

				}

			}

		}

		/* add remaining (loose) connectors into the common set */

		for (EleConnector c : looseConnectors) {

			commonStiffSet.add(c);

			stiffSetMap.put(c, commonStiffSet);

		}

	}

	@Override
	public void requireVerticalDistance(ConstraintType type, double distance, EleConnector upper, EleConnector lower) {
		// TODO Auto-generated method stub

	}

	@Override
	public void requireVerticalDistance(ConstraintType type, double distance, EleConnector upper, EleConnector base1,
			EleConnector base2) {
		// TODO Auto-generated method stub
	}

	@Override
	public void requireIncline(ConstraintType type, double incline, List<EleConnector> cs) {
		// TODO Auto-generated method stub
	}

	@Override
	public void requireSmoothness(EleConnector from, EleConnector via, EleConnector to) {
		// TODO Auto-generated method stub
	}

	@Override
	public void enforceConstraints() {

		/* assign elevation to stiff sets by averaging terrain elevation */
		// TODO what for stiff sets above the ground?
		System.out.println("DiffusionEleConstraintEnforcer:enforceConstraints");
		for (StiffConnectorSet stiffSet : stiffSetMap.values()) {

			double averageEle = 0;

			for (EleConnector connector : stiffSet) {
				averageEle += connector.getPosXYZ().y;
			}

			averageEle /= stiffSet.size();

			for (EleConnector connector : stiffSet) {
				connector.setPosXYZ(connector.pos.xyz(averageEle));
			}

		}

		Map<MapNode, EleConnector> mapNodeToEleconnector = new HashMap<MapNode, EleConnector>();
		Map<EleConnector, Double> heightMap = new HashMap<EleConnector, Double>();
		Map<EleConnector, Double> heightMap1 = new HashMap<EleConnector, Double>();

		// initialize height map

		for (EleConnector c : connectors) {
			if (c.reference != null) {
				if (c.reference instanceof MapNode) {
					MapNode mn = (MapNode) c.reference;
					mapNodeToEleconnector.put(mn, c);
				}
			} else {
				// System.out.println(c);
			}
		}

		for (EleConnector c : connectors) {
			if (c.reference instanceof MapNode) {
				MapNode mn = (MapNode) c.reference;
				List<Road> roads = RoadModule.getConnectedRoads(mn, false);// requirelanes?
				roads.forEach((road) -> {
					MapNode start = road.getPrimaryMapElement().getStartNode();
					MapNode end = road.getPrimaryMapElement().getEndNode();
					if (!mapNodeToEleconnector.containsKey(start) || !mapNodeToEleconnector.containsKey(end)) {
						System.out.println("not contained in map");
						return;
					}
					EleConnector a = mapNodeToEleconnector.get(start);
					EleConnector b = mapNodeToEleconnector.get(end);
					addConnectionToGraph(c, b);
					addConnectionToGraph(c, a);
					// AddConnection(a, b, heightMap);
				});
			}
		}
		for (EleConnector c : connectors) {
			double h = 0;
			switch (c.groundState) {
				case ABOVE:
					h = 5.0;
					break;
				case BELOW:
					h = -5.0;
					break;
				default: // stay at ground elevation
			}
			heightMap.put(c, h);
			heightMap1.put(c, h);
		}
		double dt = 0.1;
		double conductance = 1;
		for (double t = 0; t < 10; t += dt) {
			for (EleConnector c : connectors) {
				double h = heightMap.get(c);
				switch (c.groundState) {
					case ABOVE:
						h = 5.0;
						break;
					case BELOW:
						h = -5.0;
						break;
					default: // stay at ground elevation
				}
				heightMap.put(c, h);
				heightMap1.put(c, h);
			}
			for (EleConnector a : roadGraph.keySet()) {
				double dhdt = 0;
				double h = heightMap.get(a);
				for (RoadNetworkEdge edge : roadGraph.get(a)) {
					edge.setStartEnd(heightMap.get(a), heightMap.get(edge.b));
					edge.step(dt, conductance);
					heightMap1.put(a, edge.getStart());
					// heightMap1.put(edge.b, edge.getEnd());
					// dhdt -= conductance * Math.min(1, 1.0 / edge.distance) * (h -
					// heightMap.get(edge.b));
				}
				// double dhdt = 0;
				// double h = heightMap.get(a);
				// for (RoadNetworkEdge edge : roadGraph.get(a)) {
				// dhdt -= conductance * Math.min(1, 1.0 / edge.distance) * (h -
				// heightMap.get(edge.b));
				// }
				// heightMap1.put(a, dhdt * dt + h);
			}
			for (EleConnector c : connectors) {
				heightMap.put(c, heightMap1.get(c));
			}
		}

		for (EleConnector c : connectors) {
			if (c.reference instanceof MapNode) {
				MapNode mn = (MapNode) c.reference;
				List<Road> roads = RoadModule.getConnectedRoads(mn, false);// requirelanes?
				roads.forEach((road) -> {
					MapNode start = road.getPrimaryMapElement().getStartNode();
					MapNode end = road.getPrimaryMapElement().getEndNode();
					EleConnector a = mapNodeToEleconnector.get(start);
					EleConnector b = mapNodeToEleconnector.get(end);
					try {
						List<EleConnector> center = road.getCenterlineEleConnectors();
						interpolateConnection(heightMap, center, a, b, false);
					} catch (Exception e) {
						// System.out.println("center" + e.getStackTrace());
					}
					try {
						List<EleConnector> left = road.connectors.getConnectors(road.getOutlineXZ(false));
						interpolateConnection(heightMap, left, a, b, true);
					} catch (Exception e) {
					}
					try {
						List<EleConnector> right = road.connectors.getConnectors(road.getOutlineXZ(true));
						interpolateConnection(heightMap, right, a, b, true);
					} catch (Exception e) {
					}
				});
			}
		}
		for (EleConnector c : connectors) {
			double h = heightMap.get(c);
			c.setPosXYZ(c.getPosXYZ().addY(h));
		}
	}

	private void interpolateConnection(Map<EleConnector, Double> heightMap, List<EleConnector> center,
			EleConnector start, EleConnector end, boolean isSide) {
		if (center.size() == 0) {
			return;
		}
		RoadNetworkEdge edge = null;
		for (RoadNetworkEdge e : roadGraph.get(start)) {
			if (e.b == end) {
				edge = e;
				break;
			}
		}
		if (edge != null) {
			System.out.println(edge.h);
		}
		// calculate length of road
		// length will be used to calculate weights for linear interpolation
		double length = 0;
		if (isSide) {
			EleConnector before = center.get(0);
			for (int i = 0; i < center.size(); i++) {
				EleConnector segment = center.get(i);
				length += segment.getPosXYZ().distanceToXZ(before.getPosXYZ());
				before = segment;
			}
		} else {
			EleConnector before = start;
			for (int i = 0; i < center.size(); i++) {
				EleConnector segment = center.get(i);
				length += segment.getPosXYZ().distanceToXZ(before.getPosXYZ());
				before = segment;
			}
			length += end.getPosXYZ().distanceToXZ(before.getPosXYZ());
		}
		// System.out.println("length=" + length);

		{
			double pos = 0;
			double startHeight = heightMap.get(start);
			double endHeight = heightMap.get(end);
			EleConnector before = isSide ? center.get(0) : start;
			for (int i = 0; i < center.size(); i++) {
				EleConnector segment = center.get(i);
				pos += segment.getPosXYZ().distanceToXZ(before.getPosXYZ());
				heightMap.put(segment, endHeight * pos / length + startHeight * (1 - pos / length));
				before = segment;
			}
		}
	}

	// add connection to node edges
	private void addConnectionToGraph(EleConnector start, EleConnector end) {
		addConnectionToGraph(start, end, start.getPosXYZ().distanceToXZ(end.getPosXYZ()));
	}

	// add connection to node edges
	private void addConnectionToGraph(EleConnector start, EleConnector end, double distance) {
		EleConnector a = start, b = end;
		for (int i = 0; i < 2; i++) {
			if (roadGraph.containsKey(a)) {
				boolean found = false;
				List<RoadNetworkEdge> edges = roadGraph.get(a);
				for (RoadNetworkEdge edge : edges) {
					if (edge.a == a && edge.b == b) {
						found = true;
						break;
					}
					if (edge.a == b && edge.b == a) {
						found = true;
						break;
					}
				}
				if (!found) {
					edges.add(new RoadNetworkEdge(a, b, distance));
					roadGraph.put(a, edges);
				}
			} else {
				List<RoadNetworkEdge> edges = new ArrayList<RoadNetworkEdge>();
				edges.add(new RoadNetworkEdge(a, b, distance));
				roadGraph.put(a, edges);
			}
			EleConnector temp = a;
			a = b;
			b = temp;
		}
	}

	/**
	 * a set of connectors that are required to have the same elevation TODO or a
	 * precise vertical offset
	 */
	private static class StiffConnectorSet implements Iterable<EleConnector> {

		// TODO maybe look for a more efficient set implementation
		private Set<EleConnector> connectors = new HashSet<EleConnector>();

		/**
		 * adds a connector to this set, requiring it to be at the set's reference
		 * elevation
		 */
		public void add(EleConnector connector) {
			connectors.add(connector);
		}

		/**
		 * combines this set with another, and makes the other set unusable. This set
		 * will contain all {@link EleConnector}s from the other set afterwards.
		 */
		public void mergeFrom(StiffConnectorSet otherSet) {

			connectors.addAll(otherSet.connectors);

			// make sure that the other set cannot be used anymore
			otherSet.connectors = null;

		}

		public double size() {
			return connectors.size();
		}

		@Override
		public Iterator<EleConnector> iterator() {
			return connectors.iterator();
		}

	}

}