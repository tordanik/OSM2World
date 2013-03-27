package org.osm2world.core.world.network;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.EleConnector;
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
	
	private EleConnector startConnector = null;
	private EleConnector endConnector = null;

	private List<VectorXYZ> leftOutline = null;
	private List<VectorXYZ> rightOutline = null;
	
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
		
		if (startConnector == null) {
			startConnector = new EleConnector(getStartWithOffset());
			endConnector = new EleConnector(getEndWithOffset());
		}
		
		return asList(startConnector, endConnector);
		
	}
	
	/**
	 * returns the 3d start position.
	 * Only available after elevation calculation.
	 */
	protected VectorXYZ getStartXYZ() {
		return startConnector.getPosXYZ();
	}
	
	/**
	 * returns the 3d end position.
	 * Only available after elevation calculation.
	 */
	protected VectorXYZ getEndXYZ() {
		return endConnector.getPosXYZ();
	}
	
	/**
	 * returns all points along this way segment between, but not including,
	 * {@link #getStartWithOffset()} and {@link #getEndWithOffset()}
	 */
	private List<VectorXYZ> getPointsBetweenStartAndEnd() {
		
		return asList(getStartXYZ(), getEndXYZ());
		
		/* TODO  points ON a segment
		
		WaySegmentElevationProfile eleProfile = segment.getElevationProfile();
		List<VectorXYZ> pointsWithEle = eleProfile.getPointsWithEle();
		
		if (pointsWithEle.size() == 2) {
			
			return Collections.emptyList();
			
		} else {
			
			VectorXZ startWithOffset = getStartWithOffset();
			VectorXZ endWithOffset = getEndWithOffset();
			
			List<VectorXYZ> result = new ArrayList<VectorXYZ>();
			
			for (int i = 1; i < pointsWithEle.size() - 1; ++i) {
				
				VectorXYZ p = pointsWithEle.get(i);
				
				//check whether p is between start and end and
				//is sufficiently far away from them to avoid artifacts due to (almost) zero-length segments
	
				VectorXZ pXZ = p.xz();
				
				if (isBetween(pXZ, startWithOffset, endWithOffset)
						&& distanceSquared(pXZ, startWithOffset) > 0.1
						&& distanceSquared(pXZ, endWithOffset) > 0.1) {
					
					result.add(p);
					
				}
				
			}
			
			return result;
			
		}
		
		*/
			
	}
	
	/**
	 * returns a sequence of node running along the center of the
	 * line from start to end (each with offset).
	 * Uses the {@link WaySegmentElevationProfile} for adding
	 * elevation information.
	 */
	public List<VectorXYZ> getCenterline() {
		
		return getPointsBetweenStartAndEnd();
		
	}
	
	/**
	 * version of {@link #getCenterline()} in the XZ plane
	 */
	public List<VectorXZ> getCenterlineXZ() {
		
		List<VectorXZ> centerline = new ArrayList<VectorXZ>();
		
		centerline.add(getStartWithOffset());
		
		for (VectorXYZ v : getPointsBetweenStartAndEnd()) {
			centerline.add(v.xz());
		}
		
		centerline.add(getEndWithOffset());
		
		return centerline;
		
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
						
		
		//FIXME: do something like getCenterline: modify pointsWith ele using normal; then check whether the result is between the outline's start and end
		
		
		if (right) {
			
			if (rightOutline == null) {
				calculateOutlines();
			}
			
			return rightOutline;
			
		} else { //left
			
			if (leftOutline == null) {
				calculateOutlines();
			}
			
			return leftOutline;
			
		}
		
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
	
	@Override
	public SimplePolygonXZ getOutlinePolygonXZ() {
		return getOutlinePolygon().getSimpleXZPolygon();
	}

	private void calculateOutlines() {

		if (startCutVector == null || endCutVector == null) {
			throw new IllegalStateException("cannot calculate outlines before cut vectors");
		}
		
		List<VectorXYZ> centerLine = getCenterline();

		leftOutline = new ArrayList<VectorXYZ>(centerLine.size());
		rightOutline = new ArrayList<VectorXYZ>(centerLine.size());
		
		assert centerLine.size() >= 2;
		
		double halfWidth = getWidth() * 0.5f;
				
		VectorXYZ centerStart = centerLine.get(0);
		leftOutline.add(centerStart.add(startCutVector.mult(-halfWidth)));
		rightOutline.add(centerStart.add(startCutVector.mult(halfWidth)));
		
		for (int i = 1; i < centerLine.size() - 1; i++) {
			
			leftOutline.add(centerLine.get(i).add(segment.getRightNormal().mult(-halfWidth)));
			rightOutline.add(centerLine.get(i).add(segment.getRightNormal().mult(halfWidth)));
			
		}

		VectorXYZ centerEnd = centerLine.get(centerLine.size() - 1);
		leftOutline.add(centerEnd.add(endCutVector.mult(-halfWidth)));
		rightOutline.add(centerEnd.add(endCutVector.mult(halfWidth)));
		
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
		
		VectorXYZ position = (start ? startConnector : endConnector).getPosXYZ();
		VectorXZ cutVector = start ? getStartCutVector() : getEndCutVector();
		
		return position.add(cutVector.mult(
				(-0.5 + relativePosFromLeft) * getWidth()));
		
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
		return new AxisAlignedBoundingBoxXZ(getOutlinePolygon().getVertices());
	}
	
	@Override
	public String toString() {
		return "network way segment for " + segment;
	}
	
}
