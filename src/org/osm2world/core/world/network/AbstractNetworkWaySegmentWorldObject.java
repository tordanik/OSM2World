package org.osm2world.core.world.network;

import static java.lang.Double.*;
import static org.openstreetmap.josm.plugins.graphview.core.util.ValueStringParser.parseIncline;
import static org.osm2world.core.math.VectorXZ.distanceSquared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.overlaps.MapIntersectionWW;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.EleConnectorGroup;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.map_elevation.data.WaySegmentElevationProfile;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.GeometryUtil;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.datastructures.IntersectionTestObject;
import org.osm2world.core.world.data.WaySegmentWorldObject;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.data.WorldObjectWithOutline;

public abstract class AbstractNetworkWaySegmentWorldObject
	implements NetworkWaySegmentWorldObject, WaySegmentWorldObject,
	           IntersectionTestObject, WorldObjectWithOutline {

	public final MapWaySegment segment;
	
	private VectorXZ startCutVector = null;
	private VectorXZ endCutVector = null;
	
	private VectorXZ startOffset = VectorXZ.NULL_VECTOR;
	private VectorXZ endOffset = VectorXZ.NULL_VECTOR;
	
	private EleConnectorGroup connectors;

	private List<VectorXZ> leftOutlineXZ = null;
	private List<VectorXZ> rightOutlineXZ = null;
	private List<VectorXZ> outlineLoopXZ = null;
	
	private Boolean broken = null;
	
	protected AbstractNetworkWaySegmentWorldObject(MapWaySegment segment) {
		this.segment = segment;
	}
	
	@Override
	public final MapWaySegment getPrimaryMapElement() {
		return segment;
	}
	
	@Override
	public void setStartCutVector(VectorXZ cutVector) {
		this.startCutVector = cutVector;
	}
	
	@Override
	public void setEndCutVector(VectorXZ cutVector) {
		this.endCutVector = cutVector;
	}

	@Override
	public VectorXZ getStartCutVector() {
		return startCutVector;
	}

	@Override
	public VectorXZ getEndCutVector() {
		return endCutVector;
	}
	
	public VectorXZ getCutVectorAt(MapNode node) {
		if (node == segment.getStartNode()) {
			return getStartCutVector();
		} else if (node == segment.getEndNode()) {
			return getEndCutVector();
		} else {
			throw new IllegalArgumentException("node is not part of the line");
		}
	}
	
	@Override
	public void setStartOffset(VectorXZ offsetVector) {
		this.startOffset = offsetVector;
	}
	
	@Override
	public void setEndOffset(VectorXZ offsetVector) {
		this.endOffset = offsetVector;
	}
	
	protected VectorXZ getStartWithOffset() {
		return segment.getStartNode().getPos().add(startOffset); //SUGGEST (performance): cache? [also getEnd*]
	}
	
	protected VectorXZ getEndWithOffset() {
		return segment.getEndNode().getPos().add(endOffset);
	}
	
	@Override
	public EleConnectorGroup getEleConnectors() {
		
		if (isBroken()) return EleConnectorGroup.EMPTY;
		
		if (connectors == null) {
			connectors = new EleConnectorGroup();
			connectors.addConnectorsFor(getOutlinePolygonXZ().getVertices(),
					getGroundState() == GroundState.ON);
			connectors.addConnectorsFor(getCenterlineXZ(),
					getGroundState() == GroundState.ON);
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
			
			enforcer.addSameEleConstraint(center.get(i), left.get(i));
			enforcer.addSameEleConstraint(center.get(i), right.get(i));
			
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
				
				Float incline = parseIncline(inclineValue);
				
				if (incline != null) {
					if (incline > 0) {
						minIncline = incline / 100.0 * 0.5;
						maxIncline = incline / 100.0 * 1.1;
					} else if (incline < 0) {
						maxIncline = incline / 100.0 * 0.5;
						minIncline = incline / 100.0 * 1.1;
					} else {
						
						enforcer.addSameEleConstraint(center);
						
					}
				}
				
			}
			
			if (!isNaN(minIncline)) {
				enforcer.addMinInclineConstraint(center, minIncline);
			}
			
			if (!isNaN(maxIncline)) {
				enforcer.addMaxInclineConstraint(center, maxIncline);
			}
			
		}
		
		//TODO sensible maximum incline for road and rail; and waterway down-incline
		
		/* ensure a smooth transition from previous segment */
		
		//TODO this might be more elegant with an "Invisible Connector WO"
		
		//TODO take incline differences, steps etc. into account => move into Road, Rail separately
				
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
				
				List<EleConnector> previousCenter =
						previous.getCenterlineEleConnectors();
				
				enforcer.addSmoothnessConstraint(
						center.get(0),
						previousCenter.get(previousCenter.size() - 2),
						center.get(1));
				
			}

		}
		
	}
	
	List<EleConnector> getCenterlineEleConnectors() {
		return connectors.getConnectors(getCenterlineXZ());
	}
	
	/**
	 * returns a sequence of node running along the center of the
	 * line from start to end (each with offset).
	 * Uses the {@link WaySegmentElevationProfile} for adding
	 * elevation information.
	 */
	public List<VectorXZ> getCenterlineXZ() {
		
		//SUGGEST (performance): calculate only once, and store result
		
		List<VectorXZ> centerline = new ArrayList<VectorXZ>();
		
		final VectorXZ start = getStartWithOffset();
		final VectorXZ end = getEndWithOffset();
		
		centerline.add(start);
		
		// add intersections along the centerline
		
		for (MapOverlap<?,?> overlap : segment.getOverlaps()) {
			if (overlap instanceof MapIntersectionWW) {
				
				MapIntersectionWW intersection = (MapIntersectionWW) overlap;
				
				if (GeometryUtil.isBetween(intersection.pos, start, end)
						&& intersection.getOther(segment).getPrimaryRepresentation() != null) {
					centerline.add(intersection.pos);
				}
				
			}
		}
		
		// finish the centerline
		
		centerline.add(end);
		
		if (centerline.size() > 3) {
			
			// sort by distance from start
			Collections.sort(centerline, new Comparator<VectorXZ>() {
				@Override
				public int compare(VectorXZ v1, VectorXZ v2) {
					return Double.compare(
							distanceSquared(v1, start),
							distanceSquared(v2, start));
				}
			});
			
		}
		
		return centerline;
		
	}
	
	/**
	 * 3d version of {@link #getCenterlineXZ()}.
	 * Only available after elevation calculation.
	 */
	public List<VectorXYZ> getCenterline() {
		return connectors.getPosXYZ(getCenterlineXZ());
	}
	
	/**
	 * Variant of {@link #getOutline(boolean)}.
	 * This one is already available before elevation calculation.
	 */
	public List<VectorXZ> getOutlineXZ(boolean right) {
		
		if (right) {
			
			if (rightOutlineXZ == null) {
				calculateOutlines();
			}
			
			return rightOutlineXZ;
			
		} else { //left
			
			if (leftOutlineXZ == null) {
				calculateOutlines();
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
	 * 
	 * Left and right border have the same number of nodes as the elevation
	 * profile's {@link WaySegmentElevationProfile#getPointsWithEle()}.
	 * //TODO: compatible with future offset/clearing influences?
	 */
	public List<VectorXYZ> getOutline(boolean right) {
		return connectors.getPosXYZ(getOutlineXZ(right));
	}
	
	private List<VectorXZ> getOutlineLoopXZ() {
		
		if (outlineLoopXZ == null) {
			calculateOutlines();
		}
		
		return outlineLoopXZ;
		
	}
	
	@Override
	public SimplePolygonXZ getOutlinePolygonXZ() {
		
		//SUGGEST (performance) cache?
		
		if (isBroken()) {
			return null;
		} else {
			return new SimplePolygonXZ(getOutlineLoopXZ());
		}
		
	}
	
	@Override
	public PolygonXYZ getOutlinePolygon() {
		
		if (isBroken()) {
			return null;
		} else {
			return new PolygonXYZ(connectors.getPosXYZ(getOutlineLoopXZ()));
		}
		
	}
	
	private void calculateOutlines() {

		if (startCutVector == null || endCutVector == null) {
			throw new IllegalStateException("cannot calculate outlines before cut vectors");
		}
		
		// calculate left and right outlines
		
		List<VectorXZ> centerLine = getCenterlineXZ();

		leftOutlineXZ = new ArrayList<VectorXZ>(centerLine.size());
		rightOutlineXZ = new ArrayList<VectorXZ>(centerLine.size());
		
		assert centerLine.size() >= 2;
		
		double halfWidth = getWidth() * 0.5f;
				
		VectorXZ centerStart = centerLine.get(0);
		leftOutlineXZ.add(centerStart.add(startCutVector.mult(-halfWidth)));
		rightOutlineXZ.add(centerStart.add(startCutVector.mult(halfWidth)));
		
		for (int i = 1; i < centerLine.size() - 1; i++) {
			
			leftOutlineXZ.add(centerLine.get(i).add(segment.getRightNormal().mult(-halfWidth)));
			rightOutlineXZ.add(centerLine.get(i).add(segment.getRightNormal().mult(halfWidth)));
			
		}

		VectorXZ centerEnd = centerLine.get(centerLine.size() - 1);
		leftOutlineXZ.add(centerEnd.add(endCutVector.mult(-halfWidth)));
		rightOutlineXZ.add(centerEnd.add(endCutVector.mult(halfWidth)));
		
		// calculate the outline loop
		
		outlineLoopXZ = new ArrayList<VectorXZ>(centerLine.size() * 2 + 1);

		List<VectorXZ> lOutline = getOutlineXZ(false);
		List<VectorXZ> rOutline = getOutlineXZ(true);
		
		outlineLoopXZ.addAll(rOutline);
		
		lOutline = new ArrayList<VectorXZ>(lOutline);
		Collections.reverse(lOutline);
		outlineLoopXZ.addAll(lOutline);
		
		outlineLoopXZ.add(outlineLoopXZ.get(0));
		
		// check for brokenness
		
		try {
			broken = new SimplePolygonXZ(outlineLoopXZ).isClockwise();
		} catch (InvalidGeometryException e) {
			broken = true;
		}
		
	}
	
	/**
	 * checks whether this segment has a broken outline.
	 * That can happen e.g. if it lies between two junctions that are too close
	 * together.
	 */
	public boolean isBroken() {
		
		if (broken == null) {
			calculateOutlines();
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
		
		VectorXZ position = start ? getStartWithOffset() : getEndWithOffset();
		VectorXZ cutVector = start ? getStartCutVector() : getEndCutVector();
		
		return connectors.getPosXYZ(position.add(cutVector.mult(
				(-0.5 + relativePosFromLeft) * getWidth())));
		
	}

	@Override
	public VectorXZ getStartOffset() {
		return startOffset;
	}

	@Override
	public VectorXZ getEndOffset() {
		return endOffset;
	}
	
	@Override
	public VectorXZ getStartPosition() {
		return segment.getStartNode().getPos().add(getStartOffset());
	}
	
	@Override
	public VectorXZ getEndPosition() {
		return segment.getEndNode().getPos().add(getEndOffset());
	}
	
	@Override
	public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
		return new AxisAlignedBoundingBoxXZ(
				getOutlinePolygonXZ().getVertexCollection());
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + segment + ")";
	}
	
}
