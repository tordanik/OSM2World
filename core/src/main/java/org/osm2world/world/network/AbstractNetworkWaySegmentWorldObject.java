package org.osm2world.world.network;

import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toList;
import static org.osm2world.map_elevation.creation.EleConstraintEnforcer.ConstraintType.MAX;
import static org.osm2world.map_elevation.creation.EleConstraintEnforcer.ConstraintType.MIN;
import static org.osm2world.map_elevation.data.GroundState.*;
import static org.osm2world.math.VectorXZ.distance;
import static org.osm2world.math.VectorXZ.distanceSquared;
import static org.osm2world.math.algorithms.GeometryUtil.interpolateBetween;
import static org.osm2world.math.algorithms.GeometryUtil.interpolateEleOfSegment;
import static org.osm2world.util.ValueParseUtil.parseIncline;
import static org.osm2world.util.ValueParseUtil.parseLevels;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.parseInclineDirection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.*;
import org.osm2world.map_data.data.overlaps.MapIntersectionWW;
import org.osm2world.map_data.data.overlaps.MapOverlap;
import org.osm2world.map_data.data.overlaps.MapOverlapType;
import org.osm2world.map_data.data.overlaps.MapOverlapWA;
import org.osm2world.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.map_elevation.data.EleConnectorGroup;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.BoundedObject;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.algorithms.GeometryUtil;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.math.shapes.PolygonShapeXZ;
import org.osm2world.math.shapes.PolygonXYZ;
import org.osm2world.math.shapes.SimplePolygonXZ;
import org.osm2world.util.enums.UpDown;
import org.osm2world.util.exception.InvalidGeometryException;
import org.osm2world.world.attachment.AttachmentConnector;
import org.osm2world.world.data.AbstractAreaWorldObject;
import org.osm2world.world.data.WorldObject;
import org.osm2world.world.modules.BridgeModule;
import org.osm2world.world.modules.TunnelModule;

public abstract class AbstractNetworkWaySegmentWorldObject implements NetworkWaySegmentWorldObject, BoundedObject {

	private static final double CONNECTOR_SNAP_DIST = 0.001;

	public final MapWaySegment segment;

	private VectorXZ startCutLeft = null;
	private VectorXZ startCutCenter = null;
	private VectorXZ startCutRight = null;

	private VectorXZ endCutLeft = null;
	private VectorXZ endCutCenter = null;
	private VectorXZ endCutRight = null;

	protected EleConnectorGroup connectors;

	protected List<AttachmentConnector> attachmentConnectorList = emptyList();

	private List<VectorXZ> centerlineXZ = null;

	private List<VectorXZ> leftOutlineXZ = null;
	private List<VectorXZ> rightOutlineXZ = null;

	private SimplePolygonXZ outlinePolygonXZ = null;

	private Boolean broken = null;

	protected AbstractNetworkWaySegmentWorldObject(MapWaySegment segment) {
		this.segment = segment;
	}

	@Override
	public final MapWaySegment getPrimaryMapElement() {
		return segment;
	}

	@Override
	public VectorXZ getStartPosition() {
		return startCutCenter;
	}

	@Override
	public VectorXZ getEndPosition() {
		return endCutCenter;
	}

	@Override
	public void setStartCut(VectorXZ left, VectorXZ center, VectorXZ right) {
		if (left == null || center == null || right == null) { throw new IllegalArgumentException(); }
		this.startCutLeft = left;
		this.startCutCenter = center;
		this.startCutRight = right;
	}

	@Override
	public void setEndCut(VectorXZ left, VectorXZ center, VectorXZ right) {
		if (left == null || center == null || right == null) { throw new IllegalArgumentException(); }
		this.endCutLeft = left;
		this.endCutCenter = center;
		this.endCutRight = right;
	}

	/**
	 * calculates centerline and outlines, along with their connectors
	 * TODO: perform before construction, to simplify the object and avoid {@link #broken}
	 */
	private void calculateXZGeometry() {

		if (startCutCenter == null || endCutCenter == null) {
			throw new IllegalStateException("cannot calculate outlines of " + this + " before cut information is set");
		}

		connectors = new EleConnectorGroup();

		{ /* calculate centerline */

			centerlineXZ = new ArrayList<>();

			final VectorXZ start = getStartPosition();
			final VectorXZ end = getEndPosition();

			centerlineXZ.add(start);

			connectors.add(new EleConnector(start,
					segment.getStartNode(), getGroundState(segment.getStartNode())));

			// add intersections along the centerline

			for (MapOverlap<?,?> overlap : segment.getOverlaps()) {

				if (overlap.getOther(segment).getPrimaryRepresentation() == null)
					continue;

				if (overlap instanceof MapIntersectionWW) {

					MapIntersectionWW intersection = (MapIntersectionWW) overlap;

					if (GeometryUtil.isBetween(intersection.pos, start, end)) {
						if (!centerlineXZ.stream().anyMatch(p -> distance(p, intersection.pos) < CONNECTOR_SNAP_DIST)) {
							centerlineXZ.add(intersection.pos);
							connectors.add(new EleConnector(intersection.pos, null, getGroundState()));
						}
					}

				} else if (overlap instanceof MapOverlapWA
						&& overlap.type == MapOverlapType.INTERSECT) {

					if (!(overlap.getOther(segment).getPrimaryRepresentation()
							instanceof AbstractAreaWorldObject)) continue;

					MapOverlapWA overlapWA = (MapOverlapWA) overlap;

					for (int i = 0; i < overlapWA.getIntersectionPositions().size(); i++) {

						VectorXZ pos = overlapWA.getIntersectionPositions().get(i);

						if (GeometryUtil.isBetween(pos, start, end)) {
							if (!centerlineXZ.stream().anyMatch(p -> distance(p, pos) < CONNECTOR_SNAP_DIST)) {
								centerlineXZ.add(pos);
								connectors.add(new EleConnector(pos, null, getGroundState()));
							}
						}

					}

				}


			}

			// finish the centerline

			centerlineXZ.add(end);

			connectors.add(new EleConnector(end,
					segment.getEndNode(), getGroundState(segment.getEndNode())));

			if (centerlineXZ.size() > 3) {

				// sort by distance from start
				centerlineXZ.sort(comparingDouble(v -> distanceSquared(v, start)));

			}

		}

		{ /* calculate left and right outlines */

			leftOutlineXZ = new ArrayList<>(centerlineXZ.size());
			rightOutlineXZ = new ArrayList<>(centerlineXZ.size());

			assert centerlineXZ.size() >= 2;

			double halfWidth = getWidth() * 0.5;

			leftOutlineXZ.add(startCutLeft);
			rightOutlineXZ.add(startCutRight);

			connectors.add(new EleConnector(leftOutlineXZ.get(0),
					segment.getStartNode(), getGroundState(segment.getStartNode())));
			connectors.add(new EleConnector(rightOutlineXZ.get(0),
					segment.getStartNode(), getGroundState(segment.getStartNode())));

			for (int i = 1; i < centerlineXZ.size() - 1; i++) {

				leftOutlineXZ.add(centerlineXZ.get(i).add(segment.getRightNormal().mult(-halfWidth)));
				rightOutlineXZ.add(centerlineXZ.get(i).add(segment.getRightNormal().mult(halfWidth)));

				connectors.add(new EleConnector(leftOutlineXZ.get(i),
						null, getGroundState()));
				connectors.add(new EleConnector(rightOutlineXZ.get(i),
						null, getGroundState()));

			}

			leftOutlineXZ.add(endCutLeft);
			rightOutlineXZ.add(endCutRight);

			connectors.add(new EleConnector(leftOutlineXZ.get(leftOutlineXZ.size() - 1),
					segment.getEndNode(), getGroundState(segment.getEndNode())));
			connectors.add(new EleConnector(rightOutlineXZ.get(rightOutlineXZ.size() - 1),
					segment.getEndNode(), getGroundState(segment.getEndNode())));

		}

		{ /* calculate the outline loop */

			List<VectorXZ> outlineLoopXZ = new ArrayList<>(centerlineXZ.size() * 2 + 1);

			outlineLoopXZ.addAll(rightOutlineXZ);

			List<VectorXZ> left = new ArrayList<>(leftOutlineXZ);
			Collections.reverse(left);
			outlineLoopXZ.addAll(left);

			outlineLoopXZ.add(outlineLoopXZ.get(0));

			// check for brokenness

			try {
				outlinePolygonXZ = new SimplePolygonXZ(outlineLoopXZ);
				broken = outlinePolygonXZ.isClockwise();
			} catch (InvalidGeometryException e) {
				broken = true;
				connectors = EleConnectorGroup.EMPTY;
			}

		}

	}

	/**
	 * determines whether the node is connected to the terrain based on the
	 * segments connected to it
	 *
	 * @param node  one of the nodes of {@link #segment}
	 */
	private GroundState getGroundState(MapNode node) {

		WorldObject primaryWO = node.getPrimaryRepresentation();

		if (primaryWO != null) {

			return primaryWO.getGroundState();

		} else if (this.getGroundState() == ON) {

			return ON;

		} else {

			boolean allAbove = true;
			boolean allBelow = true;

			for (MapWaySegment segment : node.getConnectedWaySegments()) {
				if (segment.getPrimaryRepresentation() != null) {
					switch (segment.getPrimaryRepresentation().getGroundState()) {
					case ABOVE: allBelow = false; break;
					case BELOW: allAbove = false; break;
					case ON: return ON;
					}
				}
			}

			if (allAbove) {
				return ABOVE;
			} else if (allBelow) {
				return BELOW;
			} else {
				return ON;
			}

		}

	}

	/**
	 * implementation of {@link WorldObject#getGroundState()}.
	 * This version checks for bridge and tunnel tags to make the decision.
	 * If that is not desired, subclasses may override the method.
	 */
	@Override
	public GroundState getGroundState() {
		if (BridgeModule.isBridge(segment.getTags())) {
			return GroundState.ABOVE;
		} else if (TunnelModule.isTunnel(segment.getTags())) {
			return GroundState.BELOW;
		} else {
			return GroundState.ON;
		}
	}

	@Override
	public EleConnectorGroup getEleConnectors() {

		if (connectors == null) {
			calculateXZGeometry();
		}

		return connectors;

	}

	@Override
	public void defineEleConstraints(EleConstraintEnforcer enforcer) {

		if (isBroken()) return;

		//TODO: maybe save connectors separately right away

		List<EleConnector> center = getCenterlineEleConnectors();
		List<EleConnector> left = connectors.getConnectors(getOutlineXZ(false));
		List<EleConnector> right = connectors.getConnectors(getOutlineXZ(true));

		/* left and right connectors have the same ele as their center conn. */

		for (int i = 0; i < center.size(); i++) {

			enforcer.requireSameEle(center.get(i), left.get(i));
			enforcer.requireSameEle(center.get(i), right.get(i));

		}

		/* incline should be honored */

		String inclineValue = segment.getTags().getValue("incline");

		if (inclineValue != null) {

			double minIncline = NaN;
			double maxIncline = NaN;

			if ("up".equals(inclineValue)) {

				minIncline = 0.1;

			} else if ("down".equals(inclineValue)) {

				maxIncline = -0.1;

			} else {

				Double incline = parseIncline(inclineValue);

				if (incline != null) {
					if (incline > 0) {
						minIncline = incline / 100.0 * 0.5;
						maxIncline = incline / 100.0 * 1.1;
					} else if (incline < 0) {
						maxIncline = incline / 100.0 * 0.5;
						minIncline = incline / 100.0 * 1.1;
					} else {

						enforcer.requireSameEle(center);

					}
				}

			}

			if (!isNaN(minIncline)) {
				enforcer.requireIncline(MIN, minIncline, center);
			}

			if (!isNaN(maxIncline)) {
				enforcer.requireIncline(MAX, maxIncline, center);
			}

		}

		//TODO sensible maximum incline for road and rail; and waterway down-incline
		// ... take incline differences, steps etc. into account => move into Road, Rail separately

		/* ensure a smooth transition from previous segment */

		//TODO this might be more elegant with an "Invisible Connector WO"

		List<MapWaySegment> connectedSegments =
				segment.getStartNode().getConnectedWaySegments();

		if (connectedSegments.size() == 2) {

			MapWaySegment previousSegment = null;

			for (MapWaySegment connectedSegment : connectedSegments) {
				if (connectedSegment != this.segment) {
					previousSegment = connectedSegment;
				}
			}

			WorldObject previousWO = previousSegment.getPrimaryRepresentation();

			if (previousWO instanceof AbstractNetworkWaySegmentWorldObject) {

				AbstractNetworkWaySegmentWorldObject previous =
						(AbstractNetworkWaySegmentWorldObject)previousWO;

				if (!previous.isBroken()) {

					List<EleConnector> previousCenter =
							previous.getCenterlineEleConnectors();

					enforcer.requireSmoothness(
							previousCenter.get(previousCenter.size() - 2),
							center.get(0),
							center.get(1));

				}

			}

		}

		/* ensure smooth transitions within the way itself */

		for (int i = 0; i + 2 < center.size(); i++) {
			enforcer.requireSmoothness(
					center.get(i),
					center.get(i+1),
					center.get(i+2));
		}

	}

	protected List<EleConnector> getCenterlineEleConnectors() {

		if (isBroken()) return emptyList();

		return connectors.getConnectors(getCenterlineXZ());

	}

	/**
	 * returns a sequence of node running along the center of the
	 * line from start to end (each with offset).
	 */
	public List<VectorXZ> getCenterlineXZ() {

		if (centerlineXZ == null) {
			calculateXZGeometry();
		}

		return centerlineXZ;

	}

	/**
	 * 3d version of {@link #getCenterlineXZ()}.
	 * Only available after elevation calculation.
	 */
	public List<VectorXYZ> getCenterline() {

		if (attachmentConnectorList.size() == 2 && attachmentConnectorList.stream().allMatch(c -> c.isAttached())) {

			return interpolateEleOfSegment(getCenterlineXZ(),
					segment.getWay().getNodes().stream().map(v -> v.getPos().xz()).collect(toList()),
					attachmentConnectorList.get(0).getAttachedPos().getY(),
					attachmentConnectorList.get(1).getAttachedPos().getY());

		} else {
			return connectors.getPosXYZ(getCenterlineXZ());
		}
	}

	/**
	 * Variant of {@link #getOutline(boolean)}.
	 * This one is already available before elevation calculation.
	 */
	public List<VectorXZ> getOutlineXZ(boolean right) {

		if (right) {

			if (rightOutlineXZ == null) {
				calculateXZGeometry();
			}

			return rightOutlineXZ;

		} else { //left

			if (leftOutlineXZ == null) {
				calculateXZGeometry();
			}

			return leftOutlineXZ;

		}

	}

	/**
	 * provides the left or right border (a line at an appropriate distance
	 * from the center line), taking into account cut vectors, offsets and
	 * elevation information.
	 * Available after cut vectors, offsets and elevation information
	 * have been calculated.
	 */
	public List<VectorXYZ> getOutline(boolean right) {
		return connectors.getPosXYZ(getOutlineXZ(right));
	}

	@Override
	public SimplePolygonXZ getOutlinePolygonXZ() {

		if (outlinePolygonXZ == null) {
			calculateXZGeometry();
		}

		if (isBroken()) {
			return null;
		} else {
			return outlinePolygonXZ;
		}

	}

	public @Nullable PolygonXYZ getOutlinePolygon() {

		if (isBroken()) {
			return null;
		} else {
			return connectors.getPosXYZ(outlinePolygonXZ);
		}

	}

	@Override
	public Collection<PolygonShapeXZ> getRawGroundFootprint() {
		if (getOutlinePolygonXZ() == null) {
			return List.of();
		} else {
			return List.of(getOutlinePolygonXZ());
		}
	}

	/**
	 * checks whether this segment has a broken outline.
	 * That can happen e.g. if it lies between two junctions that are too close
	 * together.
	 */
	public boolean isBroken() {

		if (broken == null) {
			calculateXZGeometry();
		}

		//TODO filter out broken objects during creation in the world module
		return broken;

	}

	/**
	 * returns a point on the start or end cut line.
	 *
	 * @param start  point is on the start cut if true, on the end cut if false
	 * @param relativePosFromLeft  0 is the leftmost point, 1 the rightmost.
	 *                             Values in between are for interpolation.
	 */
	public VectorXYZ getPointOnCut(boolean start, double relativePosFromLeft) {

		assert 0 <= relativePosFromLeft && relativePosFromLeft <= 1;

		VectorXYZ left = connectors.getPosXYZ(start ? startCutLeft : endCutLeft);
		VectorXYZ right = connectors.getPosXYZ(start ? startCutRight : endCutRight);

		if (relativePosFromLeft == 0) {
			return left;
		} else if (relativePosFromLeft == 1) {
			return right;
		} else {
			return interpolateBetween(left, right, relativePosFromLeft);
		}

	}

	@Override
	public List<AttachmentConnector> getAttachmentConnectors() {
		return attachmentConnectorList;
	}

	/** to be used in the constructors of subclasses that wish to be attachable to indoor surfaces */
	protected void createAttachmentConnectors() {

		MapNode wayStartNode = segment.getWay().getNodes().get(0);
		MapNode wayEndNode = segment.getWay().getNodes().get(segment.getWay().getNodes().size() - 1);

		Integer firstNodeLevel = null;
		Integer lastNodeLevel = null;

		/* consider level tags on the node itself or connected elements */

		firstNodeLevel = determineLevelOfNode(wayStartNode);
		lastNodeLevel = determineLevelOfNode(wayEndNode);

		/* use segment tags */

		if (segment.getTags().containsKey("level")) {

			List<Integer> levels = parseLevels(segment.getTags().getValue("level"));
			if (levels != null) {

				if (levels.size() == 1) {
					firstNodeLevel = levels.get(0);
					lastNodeLevel = levels.get(0);
				} else {

					if (this.segment.getWay().getWaySegments().size() == 1) {
						UpDown inclineDir = parseInclineDirection(segment.getTags());
						if (inclineDir == UpDown.UP) {
							firstNodeLevel = levels.get(0);
							lastNodeLevel = levels.get(levels.size() - 1);
						} else if (inclineDir == UpDown.DOWN) {
							firstNodeLevel = levels.get(levels.size() - 1);
							lastNodeLevel = levels.get(0);
						}
					}

				}
			}
		}

		/* determine compatible surface types based on level information and other tags */

		List<String> firstNodeTypes = new ArrayList<>();
		List<String> lastNodeTypes = new ArrayList<>();

		if (firstNodeLevel != null && lastNodeLevel != null) {
			firstNodeTypes.add("floor" + firstNodeLevel);
			lastNodeTypes.add("floor" + lastNodeLevel);
		}

		if (segment.getTags().contains("location", "roof")) {
			firstNodeTypes.add("roof");
			lastNodeTypes.add("roof");
		}

		/* instantiate attachment connectors */

		if (!firstNodeTypes.isEmpty() && !lastNodeTypes.isEmpty()) {

			AttachmentConnector firstConnector = new AttachmentConnector(firstNodeTypes,
					wayStartNode.getPos().xyz(0), this, 0, false);
			AttachmentConnector lastConnector = new AttachmentConnector(lastNodeTypes,
					wayEndNode.getPos().xyz(0), this, 0, false);

			attachmentConnectorList = asList(firstConnector, lastConnector);
		}

	}

	/**
	 * tries to find out which level a node is on.
	 * Considers level=* tags on the node itself as well as on connected elements.
	 *
	 * @return  the level value, or null if it could not be determined
	 */
	private static final Integer determineLevelOfNode(MapNode node) {

		/* use node tags */

		if (node.getTags().containsKey("level")) {
			List<Integer> parsedLevels = parseLevels(node.getTags().getValue("level"));
			if (parsedLevels != null && parsedLevels.size() == 1) {
				return parsedLevels.get(0);
			}
		}

		/* as a fallback, find level tags of connected areas */

		Integer result = null;

		for (MapElement connectedElement : node.getAdjacentAreas()) {

			TagSet tags = connectedElement.getTags();
			List<Integer> parsedLevels = parseLevels(tags.getValue("level"));

			if (parsedLevels != null && parsedLevels.size() == 1) {
				int level = parsedLevels.get(0);
				if (result == null || level < result) {
					result = level;
				}
			}

		}

		if (result != null) {
			return result;
		}

		/* as another fallback, find level tags of connected ways (may have incline tags) */

		for (MapWaySegment connectedSegment : node.getConnectedWaySegments()) {

			MapWay connectedWay = connectedSegment.getWay();

			TagSet tags = connectedSegment.getTags();
			List<Integer> parsedLevels = parseLevels(tags.getValue("level"));
			if (parsedLevels == null) continue;

			Integer level = null;

			if (parsedLevels.size() == 1) {
				level = parsedLevels.get(0);
			} else if (parsedLevels.size() > 1) {

				UpDown inclineDir = parseInclineDirection(tags);

				if (node == connectedWay.getNodes().get(0)) {
					if (inclineDir == UpDown.UP) {
						level = min(parsedLevels);
					} else if (inclineDir == UpDown.DOWN) {
						level = max(parsedLevels);
					}
				} else if (node == connectedWay.getNodes().get(connectedWay.getNodes().size() - 1)) {
					if (inclineDir == UpDown.UP) {
						level = max(parsedLevels);
					} else if (inclineDir == UpDown.DOWN) {
						level = min(parsedLevels);
					}
				}
			}

			if (result == null || level != null && level < result) {
				result = level;
			}

		}

		return result;

	}

	@Override
	public AxisAlignedRectangleXZ boundingBox() {

		if (isBroken() || getOutlinePolygonXZ() == null) {
			return null;
		} else {
			return getOutlinePolygonXZ().boundingBox();
		}

	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + segment + ")";
	}

}
