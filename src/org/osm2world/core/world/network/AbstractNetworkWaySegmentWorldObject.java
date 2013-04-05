package org.osm2world.core.world.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.EleConnectorGroup;
import org.osm2world.core.map_elevation.data.WaySegmentElevationProfile;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.datastructures.IntersectionTestObject;
import org.osm2world.core.world.data.WaySegmentWorldObject;
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
	public Iterable<EleConnector> getEleConnectors() {
		
		if (connectors == null) {
			connectors = new EleConnectorGroup();
			connectors.addConnectorsFor(getOutlinePolygonXZ().getVertices());
			connectors.addConnectorsFor(getCenterlineXZ());
		}
		
		return connectors;
		
	}
	
	@Override
	public void addEleConstraints(EleConstraintEnforcer enforcer) {
		
		//TODO: maybe save connectors separately right away
		
		// left and right connectors have the same ele as their center conn.
		
		List<VectorXZ> center = getCenterlineXZ();
		List<VectorXZ> left = getOutlineXZ(false);
		List<VectorXZ> right = getOutlineXZ(true);
		//TODO assure same length
		
		for (int i = 0; i < center.size(); i++) {
			
			EleConnector connCenter = connectors.getConnector(center.get(i));
			EleConnector connLeft = connectors.getConnector(left.get(i));
			EleConnector connRight = connectors.getConnector(right.get(i));
			
			enforcer.addSameEleConstraint(connCenter, connLeft);
			enforcer.addSameEleConstraint(connCenter, connRight);
			
		}
		
	}
	
	/**
	 * returns a sequence of node running along the center of the
	 * line from start to end (each with offset).
	 * Uses the {@link WaySegmentElevationProfile} for adding
	 * elevation information.
	 */
	public List<VectorXZ> getCenterlineXZ() {
		
		List<VectorXZ> centerline = new ArrayList<VectorXZ>();
		
		centerline.add(getStartWithOffset());
		
		centerline.add(getEndWithOffset());
		
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
	
	@Override
	public SimplePolygonXZ getOutlinePolygonXZ() {
		
		List<VectorXZ> outline = new ArrayList<VectorXZ>();

		List<VectorXZ> lOutline = getOutlineXZ(false);
		List<VectorXZ> rOutline = getOutlineXZ(true);
		
		outline.addAll(lOutline);
		
		rOutline = new ArrayList<VectorXZ>(rOutline);
		Collections.reverse(rOutline);
		outline.addAll(rOutline);
		
		outline.add(outline.get(0));
		
		return new SimplePolygonXZ(outline);
		
	}
	
	@Override
	public PolygonXYZ getOutlinePolygon() {
		
		List<VectorXYZ> outline = new ArrayList<VectorXYZ>();

		List<VectorXYZ> lOutline = getOutline(false);
		List<VectorXYZ> rOutline = getOutline(true);
		
		outline.addAll(lOutline);
		
		rOutline = new ArrayList<VectorXYZ>(rOutline);
		Collections.reverse(rOutline);
		outline.addAll(rOutline);
		
		outline.add(outline.get(0));
		
		return new PolygonXYZ(outline);
		
	}
	
	private void calculateOutlines() {

		if (startCutVector == null || endCutVector == null) {
			throw new IllegalStateException("cannot calculate outlines before cut vectors");
		}
		
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
		
		//TODO: what if adding the cut vector moves start/end *BEHIND* the
		//outline node from an intersection-induced node?
		
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
