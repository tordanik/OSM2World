package org.osm2world.core.world.network;

import static java.lang.Math.PI;
import static java.util.Comparator.comparingDouble;
import static org.osm2world.core.math.VectorXZ.angleBetween;
import static org.osm2world.core.math.VectorXZ.distance;
import static org.osm2world.core.math.algorithms.GeometryUtil.getLineIntersection;
import static org.osm2world.core.math.algorithms.GeometryUtil.projectPerpendicular;
import static org.osm2world.core.world.network.NetworkUtil.getConnectedNetworkSegments;

import java.util.*;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.osm2world.core.conversion.ConversionLog;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.LineSegmentXZ;
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.network.JunctionNodeWorldObject.JunctionSegmentInterface;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * class that will calculate the information for those {@link MapElement}s
 * that implement one of the relevant interfaces from this package and set it.
 * Calling this is necessary for those representations to work properly!
 */
public class NetworkCalculator {

	private NetworkCalculator() {}

	private static final double PARALLEL_ROAD_THRESHOLD_ANGLE = PI / 18;
	private static final double JUNCTION_OUTLINE_SNAP_DISTANCE = 0.01;

	/**
	 * calculates cut and offset information for all Network*WorldObjects of elements in the dataset.
	 */
	public static void calculateNetworkInformationInMapData(MapData mapData) {

		for (MapNode node : mapData.getMapNodes()) {

			Set<NetworkWaySegmentWorldObject> unhandledNetworkSegments = new HashSet<>(
					getConnectedNetworkSegments(node, NetworkWaySegmentWorldObject.class, null));

			Predicate<NetworkWaySegmentWorldObject> isInbound = s -> node.getInboundLines().contains(s.getPrimaryMapElement());
			Predicate<NetworkWaySegmentWorldObject> isOutbound = isInbound.negate();

			for (NodeWorldObject nodeWorldObject : node.getRepresentations()) {

				if (nodeWorldObject instanceof JunctionNodeWorldObject) {

					JunctionNodeWorldObject<?> junction = (JunctionNodeWorldObject<?>)nodeWorldObject;

					calculateJunctionNodeEffects(node, junction);

					unhandledNetworkSegments.removeAll(junction.getConnectedNetworkSegments());

				} else if (nodeWorldObject instanceof VisibleConnectorNodeWorldObject) {

					VisibleConnectorNodeWorldObject<?> connector = (VisibleConnectorNodeWorldObject<?>) nodeWorldObject;

					if (connector.getConnectedNetworkSegments().size() != 2) {
						ConversionLog.warn("Illegal number of connected segments for " + node + ": "
								+ connector.getConnectedNetworkSegments(), node);
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
			NetworkWaySegmentWorldObject segment1, NetworkWaySegmentWorldObject segment2,
			boolean inbound1, boolean inbound2) {

		MapWaySegment line1 = segment1.getPrimaryMapElement();
		MapWaySegment line2 = segment2.getPrimaryMapElement();

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

		/* perform calculations necessary for connectors
		 * whose representation requires space */

		double connectorLength = 0;

		if (visibleConnectorRep != null) {
			connectorLength = visibleConnectorRep.getLength();
		}

		//connected node of line1 is moved orthogonally to the cut vector
		VectorXZ offset1 = cutVector.rightNormal().mult(connectorLength / 2);

		if (inbound1) {
			VectorXZ center = segment1.getPrimaryMapElement().getEndNode().getPos();
			center = center.add(offset1);
			VectorXZ toRight = cutVector.mult(segment1.getWidth() * 0.5);
			segment1.setEndCut(center.subtract(toRight), center, center.add(toRight));
		} else {
			VectorXZ center = segment1.getPrimaryMapElement().getStartNode().getPos();
			center = center.add(offset1);
			VectorXZ toRight = cutVector.mult(-segment1.getWidth() * 0.5);
			segment1.setStartCut(center.subtract(toRight), center, center.add(toRight));
		}

		//connected node of line2 is moved into the opposite direction
		VectorXZ offset2 = offset1.invert();

		if (inbound2) {
			VectorXZ center = segment2.getPrimaryMapElement().getEndNode().getPos();
			center = center.add(offset2);
			VectorXZ toRight = cutVector.mult(-segment2.getWidth() * 0.5);
			segment2.setEndCut(center.subtract(toRight), center, center.add(toRight));
		} else {
			VectorXZ center = segment2.getPrimaryMapElement().getStartNode().getPos();
			center = center.add(offset2);
			VectorXZ toRight = cutVector.mult(segment2.getWidth() * 0.5);
			segment2.setStartCut(center.subtract(toRight), center, center.add(toRight));
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
					segment1.getWidth(),
					segment2.getWidth());

		}

		//TODO: if done properly, this might affect NOT ONLY the directly adjacent lines

	}

	/**
	 * calculates the polygon covered by a junction
	 */
	private static void calculateJunctionNodeEffects(MapNode junctionNode, JunctionNodeWorldObject<?> junction) {

		List<? extends NetworkWaySegmentWorldObject> networkSegments = junction.getConnectedNetworkSegments();

		/* Step 1: Find intersections between the (extended) sides of neighboring way segments.
		 * For segments which are (almost) parallel, null is inserted as the intersection.
		 * Intersection i is the one between segment i and segment i + 1. */

		List<VectorXZ> intersections = new ArrayList<>(networkSegments.size());

		for (int i = 0; i < networkSegments.size(); i++) {

			NetworkWaySegmentWorldObject s = networkSegments.get(i);
			NetworkWaySegmentWorldObject t = networkSegments.get((i + 1) % networkSegments.size());

			// determine a point and direction vector for the *left* edge of s

			VectorXZ sEdgeDirection = s.getPrimaryMapElement().getDirection();
			VectorXZ sVectorToEdge = s.getPrimaryMapElement().getRightNormal().mult(-0.5 * s.getWidth());

			if (s.getPrimaryMapElement().getStartNode() == junctionNode) {
				// outbound segment, flip the vectors
				sEdgeDirection = sEdgeDirection.invert();
				sVectorToEdge = sVectorToEdge.invert();
			}

			VectorXZ sEdgePos = s.getPrimaryMapElement().getCenter().add(sVectorToEdge);

			// determine a point and direction vector for the *right* edge of t

			VectorXZ tEdgeDirection = t.getPrimaryMapElement().getDirection();
			VectorXZ tVectorToEdge = t.getPrimaryMapElement().getRightNormal().mult(+0.5 * t.getWidth());

			if (t.getPrimaryMapElement().getStartNode() == junctionNode) {
				// outbound segment, flip the vectors
				tEdgeDirection = tEdgeDirection.invert();
				tVectorToEdge = tVectorToEdge.invert();
			}

			VectorXZ tEdgePos = t.getPrimaryMapElement().getCenter().add(tVectorToEdge);

			// find the intersection unless the segments are (almost) parallel

			if (angleBetween(sEdgeDirection, tEdgeDirection.invert()) < PARALLEL_ROAD_THRESHOLD_ANGLE) {
				intersections.add(null);
			} else {
				intersections.add(getLineIntersection(sEdgePos, sEdgeDirection, tEdgePos, tEdgeDirection));
			}

		}

		/* Step 2: Project the intersections (and the junction node) onto the segment.
		 * Pick the one farthest "back" as the place where the segment ends and the junction begins. */

		List<VectorXZ> cutPoints = new ArrayList<>();

		for (int i = 0; i < networkSegments.size(); i++) {

			LineSegmentXZ lineSegment = networkSegments.get(i).getPrimaryMapElement().getLineSegment();

			List<VectorXZ> candidatePositions = new ArrayList<>();
			candidatePositions.add(junctionNode.getPos());
			candidatePositions.add(intersections.get(i));
			candidatePositions.add(intersections.get((i-1 + intersections.size()) % intersections.size()));
			candidatePositions.removeIf(Objects::isNull);

			// project them onto the segment's infinite line
			candidatePositions.replaceAll(p -> projectPerpendicular(p, lineSegment.p1, lineSegment.p2));

			// find the one with the largest distance from a point very (200 m) far beyond the junction
			// (cannot just use distance from junction because some points might be beyond the junction)
			VectorXZ referencePoint = lineSegment.getCenter().add(
					junctionNode.getPos().subtract(lineSegment.getCenter()).normalize().mult(200+1));
			VectorXZ point = candidatePositions.stream().max(comparingDouble(p -> distance(p, referencePoint))).get();

			cutPoints.add(point);

		}

		/* calculate the interfaces between this junction and connected network segments */

		List<JunctionSegmentInterface> segmentInterfaces = new ArrayList<>();
		ArrayList<VectorXZ> pointsBetween = new ArrayList<>();

		for (int i = 0; i < junction.getConnectedNetworkSegments().size(); i++) {

			NetworkWaySegmentWorldObject networkSegment = networkSegments.get(i);
			MapWaySegment segment = networkSegment.getPrimaryMapElement();

			VectorXZ cutPoint = cutPoints.get(i);
			VectorXZ scaledCutVector = segment.getRightNormal().mult(networkSegment.getWidth() * 0.5);

			if (segment.getEndNode() == junctionNode) {
				scaledCutVector = scaledCutVector.invert();
			}

			segmentInterfaces.add(new JunctionSegmentInterface(
					cutPoint.subtract(scaledCutVector), cutPoint.add(scaledCutVector)));

			pointsBetween.add(intersections.get(i));

		}

		/* filter out or merge points that are too close to each other */

		for (int i = 0; i < segmentInterfaces.size(); i++) {
			JunctionSegmentInterface sA = segmentInterfaces.get(i);
			JunctionSegmentInterface sB = segmentInterfaces.get((i + 1) % segmentInterfaces.size());
			if (sA == null || sB == null) continue;
			if (sB.leftContactPos.distanceTo(sA.rightContactPos) < JUNCTION_OUTLINE_SNAP_DISTANCE) {
				sA = new JunctionSegmentInterface(sA.leftContactPos, sB.leftContactPos);
				segmentInterfaces.set(i, sA);
			}
		}

		for (int i = 0; i < pointsBetween.size(); i++) {
			VectorXZ pointBetween = pointsBetween.get(i);
			if (pointBetween != null && segmentInterfaces.stream().anyMatch(s -> s != null
					&& (s.leftContactPos.distanceTo(pointBetween) < JUNCTION_OUTLINE_SNAP_DISTANCE
					|| s.rightContactPos.distanceTo(pointBetween) < JUNCTION_OUTLINE_SNAP_DISTANCE))) {
				pointsBetween.set(i, null);
			}
		}

		/* provide the results to the junction as well as the connected segments */

		junction.setInformation(segmentInterfaces, pointsBetween);

		for (int i = 0; i < junction.getConnectedNetworkSegments().size(); i++) {

			NetworkWaySegmentWorldObject networkSegment = networkSegments.get(i);
			MapWaySegment segment = networkSegment.getPrimaryMapElement();

			VectorXZ cutPoint = cutPoints.get(i);
			JunctionSegmentInterface segmentInterface = segmentInterfaces.get(i);

			if (segment.getStartNode() == junctionNode) {
				networkSegment.setStartCut(segmentInterface.leftContactPos, cutPoint, segmentInterface.rightContactPos);
			} else {
				networkSegment.setEndCut(segmentInterface.rightContactPos, cutPoint, segmentInterface.leftContactPos);
			}

		}

	}

	private static void setOrthogonalCutVector(NetworkWaySegmentWorldObject s, boolean start) {

		VectorXZ toRight = s.getPrimaryMapElement().getRightNormal().mult(s.getWidth() * 0.5);

		if (start) {
			VectorXZ center = s.getPrimaryMapElement().getStartNode().getPos();
			s.setStartCut(center.subtract(toRight), center, center.add(toRight));
		} else {
			VectorXZ center = s.getPrimaryMapElement().getEndNode().getPos();
			s.setEndCut(center.subtract(toRight), center, center.add(toRight));
		}

	}

}
