package org.osm2world.core.world.network;

import static org.osm2world.core.world.network.NetworkUtil.getConnectedNetworkSegments;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapSegment;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.network.JunctionNodeWorldObject.JunctionSegmentInterface;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * class that will calculate the information for those {@link MapElement}s
 * that implement an interface from org.o3dw.representation.network and set it.
 * Calling this is necessary for those representations to work properly!
 */
public class NetworkCalculator {

	private NetworkCalculator() {}

	private static final float ROAD_PUSHING_STEP = 0.01f;

	/**
	 * calculates cut and offset information for all
	 * NetworkNode/Line/AreaRepresentations of elements in a grid.
	 */
	public static void calculateNetworkInformationInGrid(MapData grid) {

		for (MapNode node : grid.getMapNodes()) {

			Set<NetworkWaySegmentWorldObject> unhandledNetworkSegments = new HashSet<>(
					getConnectedNetworkSegments(node, NetworkWaySegmentWorldObject.class, null));

			Predicate<NetworkWaySegmentWorldObject> isInbound = s -> node.getInboundLines().contains(s.getPrimaryMapElement());
			Predicate<NetworkWaySegmentWorldObject> isOutbound = isInbound.negate();

			for (NodeWorldObject nodeWorldObject : node.getRepresentations()) {

				if (nodeWorldObject instanceof JunctionNodeWorldObject) {

					JunctionNodeWorldObject<?> junction = (JunctionNodeWorldObject<?>)nodeWorldObject;

					calculateJunctionNodeEffects(node, junction,
							junction.getConnectedNetworkSegments(isInbound),
							junction.getConnectedNetworkSegments(isOutbound));

					unhandledNetworkSegments.removeAll(junction.getConnectedNetworkSegments());

				} else if (nodeWorldObject instanceof VisibleConnectorNodeWorldObject) {

					VisibleConnectorNodeWorldObject<?> connector = (VisibleConnectorNodeWorldObject<?>) nodeWorldObject;

					if (connector.getConnectedNetworkSegments().size() != 2) {
						System.err.println("Illegal number of connected segments for " + node + ": "
								+ connector.getConnectedNetworkSegments());
						continue;
					}

					NetworkWaySegmentWorldObject s1 = connector.getConnectedNetworkSegments().get(0);
					NetworkWaySegmentWorldObject s2 = connector.getConnectedNetworkSegments().get(1);

					calculateConnectorNodeEffects(connector, s1, s2, isInbound.test(s1), isInbound.test(s2));

					unhandledNetworkSegments.removeAll(connector.getConnectedNetworkSegments());

				}

			}

			/* handle the remaining network segments (untagged connecting nodes may not be turned into a WorldObject) */

			Multimap<?, NetworkWaySegmentWorldObject> netSegmentsByClass =
					Multimaps.index(unhandledNetworkSegments, Object::getClass);

			netSegmentsByClass.asMap().forEach((k, networkSegments) -> {

				if (networkSegments.size() == 2) {

					/* exactly 2 segments of the same type connecting to each other, e.g. in the middle of a road way */

					List<NetworkWaySegmentWorldObject> segmentList = new ArrayList<>(networkSegments);
					NetworkWaySegmentWorldObject s1 = segmentList.get(0);
					NetworkWaySegmentWorldObject s2 = segmentList.get(1);

					calculateConnectorNodeEffects(null, s1, s2, isInbound.test(s1), isInbound.test(s2));

				} else {

					for (NetworkWaySegmentWorldObject nSegment : networkSegments) {
						setOrthogonalCutVector(nSegment, isOutbound.test(nSegment));
					}

				}

			});

		}

	}

	/**
	 * calculates the effects of both visible and invisible connector nodes.
	 */
	private static void calculateConnectorNodeEffects(
			@Nullable VisibleConnectorNodeWorldObject<?> visibleConnectorRep,
			NetworkWaySegmentWorldObject renderable1, NetworkWaySegmentWorldObject renderable2,
			boolean inbound1, boolean inbound2) {

		MapWaySegment line1 = renderable1.getPrimaryMapElement();
		MapWaySegment line2 = renderable2.getPrimaryMapElement();

		/* calculate cut as angle bisector between the two lines */

		VectorXZ inVector = line1.getDirection();
		VectorXZ outVector = line2.getDirection();

		if (!inbound1) { inVector = inVector.invert(); }
		if (inbound2) { outVector = outVector.invert(); }

		VectorXZ cutVector;

		if (inVector.equals(outVector)) { //TODO: allow for some small difference?
			cutVector = outVector.rightNormal();
		} else {
			cutVector = outVector.subtract(inVector);
			cutVector = cutVector.normalize();
		}

		//make sure that cutVector points to the right, which is equivalent to:
		//y component of the cross product (inVector x cutVector) is positive.
		//If this isn't the case, invert the cut vector.
		if (inVector.z * cutVector.x - inVector.x * cutVector.z <= 0) {
			cutVector = cutVector.invert();
		}

		/* set calculated cut vector */

		if (inbound1) {
			renderable1.setEndCutVector(cutVector);
		} else {
			renderable1.setStartCutVector(cutVector.invert());
		}

		if (inbound2) {
			renderable2.setEndCutVector(cutVector.invert());
		} else {
			renderable2.setStartCutVector(cutVector);
		}

		/* perform calculations necessary for connectors
		 * whose representation requires space */

		double connectorLength = 0;

		if (visibleConnectorRep != null) {
			connectorLength = visibleConnectorRep.getLength();
		}

		if (connectorLength > 0) {

			/* move connected lines to make room for the node's representation */

			//connected node of line1 is moved orthogonally to the cut vector
			VectorXZ offset1 = cutVector.rightNormal();
			offset1 = offset1.mult(connectorLength / 2);
			if (inbound1) {
				renderable1.setEndOffset(offset1);
			} else {
				renderable1.setStartOffset(offset1);
			}

			//node of line2 is moved into the opposite direction
			VectorXZ offset2 = offset1.invert();
			if (inbound2) {
				renderable2.setEndOffset(offset2);
			} else {
				renderable2.setStartOffset(offset2);
			}

			/* provide information to node's representation */

			if (visibleConnectorRep != null) {

				VectorXZ connectedPos1;
				VectorXZ connectedPos2;

				if (inbound1) {
					connectedPos1 = line1.getEndNode().getPos();
				} else {
					connectedPos1 = line1.getStartNode().getPos();
				}

				if (inbound2) {
					connectedPos2 = line2.getEndNode().getPos();
				} else {
					connectedPos2 = line2.getStartNode().getPos();
				}

				visibleConnectorRep.setInformation(
						cutVector,
						connectedPos1.add(offset1),
						connectedPos2.add(offset2),
						renderable1.getWidth(),
						renderable2.getWidth());

			}

			//TODO: if done properly, this might affect NOT ONLY the directly adjacent lines

		}

	}

	private static void calculateJunctionNodeEffects(MapNode node, JunctionNodeWorldObject<?> nodeRepresentation,
			List<? extends NetworkWaySegmentWorldObject> inboundNSegments,
			List<? extends NetworkWaySegmentWorldObject> outboundNSegments) {

		/* create list of all connected segments which have a network segment representation.
		 * Order of adds is important, it needs to match the order of cutVectors, coords and widths adds. */

		List<MapWaySegment> connectedNSegments = new ArrayList<>();
		inboundNSegments.forEach(s -> connectedNSegments.add(s.getPrimaryMapElement()));
		outboundNSegments.forEach(s -> connectedNSegments.add(s.getPrimaryMapElement()));

		//all cut vectors in here will point to the right from the junction's pov!
		List<VectorXZ> cutVectors = new ArrayList<VectorXZ>(connectedNSegments.size());
		List<VectorXZ> coords = new ArrayList<VectorXZ>(connectedNSegments.size());
		List<Float> widths = new ArrayList<Float>(connectedNSegments.size());

		/* determine cut angles: always orthogonal to the connected line */

		for (NetworkWaySegmentWorldObject inNSegment : inboundNSegments) {

			MapSegment in = inNSegment.getPrimaryMapElement();

			VectorXZ cutVector = in.getRightNormal();
			inNSegment.setEndCutVector(cutVector);
			cutVectors.add(cutVector.invert());

			coords.add(in.getEndNode().getPos());
			widths.add(inNSegment.getWidth());

		}

		for (NetworkWaySegmentWorldObject outNSegment : outboundNSegments) {

			MapSegment out = outNSegment.getPrimaryMapElement();

			VectorXZ cutVector = out.getRightNormal();
			outNSegment.setStartCutVector(cutVector);
			cutVectors.add(cutVector);

			coords.add(out.getStartNode().getPos());
			widths.add(outNSegment.getWidth());

		}

		/* move roads away from the intersection until they cannot overlap anymore,
		 * this is certain if the distance between their ends' center points
		 * is greater than the sum of their half-widths */

		boolean overlapPossible;

		do {

			overlapPossible = false;

			overlapCheck:
			for (int r1=0; r1 < coords.size(); r1++) {
				for (int r2=r1+1; r2 < coords.size(); r2++) {

					/* ignore overlapping (or almost overlapping) way segments
					 * as no reasonable amount of pushing would separate these */
					if (VectorXZ.distance(connectedNSegments.get(r1).getDirection(),
							connectedNSegments.get(r2).getDirection()) < 0.1
						||	VectorXZ.distance(connectedNSegments.get(r1).getDirection(),
								connectedNSegments.get(r2).getDirection().invert()) < 0.1) {
						continue;
					}

					double distance = Math.abs(coords.get(r1).subtract(coords.get(r2)).length());

					if (distance > 200) {
						//TODO: proper error handling
						System.err.println("distance has exceeded 200 at node " + node
								+ "\n (representation: " + nodeRepresentation + ")");
						// overlapCheck will remain false, no further size increase
						break overlapCheck;
					}

					if (distance <= widths.get(r1)*0.5 + widths.get(r2)*0.5) {
						overlapPossible = true;
						break overlapCheck;
					}

				}
			}

			if (overlapPossible) {

				/* push outwards */

				coords.clear();

				for (NetworkWaySegmentWorldObject inNSegment : inboundNSegments) {

					MapSegment in = inNSegment.getPrimaryMapElement();

					VectorXZ offsetModification = in.getDirection().mult(-ROAD_PUSHING_STEP);

					VectorXZ newEndOffset = inNSegment.getEndOffset().add(offsetModification);
					inNSegment.setEndOffset(newEndOffset);
					coords.add(in.getEndNode().getPos().add(newEndOffset));

				}

				for (NetworkWaySegmentWorldObject outNSegment : outboundNSegments) {

					MapSegment out = outNSegment.getPrimaryMapElement();

					VectorXZ offsetModification = out.getDirection().mult(ROAD_PUSHING_STEP);

					VectorXZ newStartOffset = outNSegment.getStartOffset().add(offsetModification);
					outNSegment.setStartOffset(newStartOffset);
					coords.add(out.getStartNode().getPos().add(newStartOffset));

				}

			}

		} while(overlapPossible);

		/* set calculated information using the correct order */

		List<MapSegment> segments = node.getConnectedSegments();

		List<JunctionSegmentInterface> segmentInterfaces = new ArrayList<>(segments.size());

		for (MapSegment segment : segments) {

			if (connectedNSegments.contains(segment)) {

				int index = connectedNSegments.indexOf(segment);

				segmentInterfaces.add(new JunctionSegmentInterface(
						coords.get(index).subtract(cutVectors.get(index).mult(widths.get(index) * 0.5f)),
						coords.get(index).add(cutVectors.get(index).mult(widths.get(index) * 0.5f))));

			} else {
				segmentInterfaces.add(null);
			}

		}

		nodeRepresentation.setInformation(segmentInterfaces);

	}

	private static void setOrthogonalCutVector(NetworkWaySegmentWorldObject s, boolean setStartVector) {

		VectorXZ cutVector = s.getPrimaryMapElement().getRightNormal();

		if (setStartVector) {
			s.setStartCutVector(cutVector);
		} else {
			s.setEndCutVector(cutVector);
		}

	}

}
