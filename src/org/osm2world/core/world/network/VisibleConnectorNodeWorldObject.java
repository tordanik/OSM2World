package org.osm2world.core.world.network;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapSegment;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.PolygonXYZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.datastructures.IntersectionTestObject;
import org.osm2world.core.world.creation.NetworkCalculator;
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.data.WorldObjectWithOutline;

public abstract class VisibleConnectorNodeWorldObject
	implements NodeWorldObject, IntersectionTestObject, WorldObjectWithOutline {

	protected final MapNode node;

	protected boolean informationProvided;
	
	protected VectorXZ cutVector;
	protected VectorXZ startPos;
	protected VectorXZ endPos;
	protected float startWidth;
	protected float endWidth;

	/**
	 * returns the length required by this node representation.
	 * Adjacent lines will be pushed back accordingly.
	 * 
	 * If this is 0, this has the same effect as an invisible
	 * connector node (adjacent line representations
	 * directly touching each other). Examples where non-zero values
	 * are needed include crossings at nodes in roads.
	 * 
	 * Needs to be provided by the implementing class before the
	 * calculation in {@link NetworkCalculator} starts.
	 */
	abstract public float getLength();
	
	/**
	 * sets the results of {@link NetworkCalculator}'s calculations.
	 * Most methods in this class cannot be used until this method
	 * has provided the required information!
	 */
	public void setInformation(VectorXZ cutVector,
			VectorXZ startPos, VectorXZ endPos,
			float startWidth, float endWidth) {
		
		this.informationProvided = true;
		
		this.cutVector = cutVector;
		this.startPos = startPos;
		this.endPos = endPos;
		this.startWidth = startWidth;
		this.endWidth = endWidth;
		
	}
	
	public VisibleConnectorNodeWorldObject(MapNode node) {
		this.node = node;
	}
	
	@Override
	public final MapElement getPrimaryMapElement() {
		return node;
	}
	
	/**
	 * variant of {@link #getOutline(int, int)} for the XZ plane
	 */
	public List<VectorXZ> getOutlineXZ(int from, int to) {

		checkInformationProvided();
		
		List<VectorXZ> outline = new ArrayList<VectorXZ>();

		List<MapSegment> segments = node.getConnectedSegments();

		assert from >= 0 && from < segments.size();
		assert to >= 0 && to < segments.size();

		if (((from == 1 && to == 0) || (from == 0 && to == 1))) {
						
			if (from == 0) {
				
				VectorXZ pos1 = startPos
					.add(cutVector.mult(startWidth));

				VectorXZ pos2 = endPos
					.add(cutVector.mult(endWidth));
				
				outline.add(pos1);
				outline.add(pos2);
				
			} else {

				VectorXZ pos1 = endPos
					.subtract(cutVector.mult(endWidth));

				VectorXZ pos2 = startPos
					.subtract(cutVector.mult(startWidth));
				
				outline.add(pos1);
				outline.add(pos2);
				
			}
			
		} else if (from == to
				&& segments.get(from) instanceof MapWaySegment) { //usually at the end of a noexit road
			
			MapWaySegment segment = (MapWaySegment) segments.get(from);
			
			if (segment.getPrimaryRepresentation() instanceof NetworkWaySegmentWorldObject) {
				
				NetworkWaySegmentWorldObject rep =
					(NetworkWaySegmentWorldObject) segment.getPrimaryRepresentation();
				
				//TODO: the calculations for pos1/2 should be part of the NetworkLineRepresentation (it's used quite often)
				
				if (segment.getEndNode() == node) { //inbound segment
					
					VectorXZ pos1 = node.getPos()
						.add(rep.getEndOffset())
						.add(rep.getEndCutVector().mult(rep.getWidth()/2));
					
					VectorXZ pos2 = node.getPos()
						.add(rep.getEndOffset())
						.subtract(rep.getEndCutVector().mult(rep.getWidth()/2));
										
					outline.add(pos1);
					outline.add(pos2);
					
				} else { //outbound segment

					VectorXZ pos1 = node.getPos()
						.add(rep.getStartOffset())
						.subtract(rep.getStartCutVector().mult(rep.getWidth()/2));
					
					VectorXZ pos2 = node.getPos()
						.add(rep.getStartOffset())
						.add(rep.getStartCutVector().mult(rep.getWidth()/2));

					outline.add(pos1);
					outline.add(pos2);
										
				}
				
			}
			
		}
		
		return outline;
		
	}
	
	/**
	 * provides outline for the areas covered by the connector.
	 * 
	 * The from and to indices refer to the list
	 * returned by the underlying {@link MapNode}'s
	 * {@link MapNode#getConnectedSegments()} method.
	 * 
	 * TODO: method necessary after Terrain change?
	 */
	public List<VectorXYZ> getOutline(int from, int to) {
		
		return node.getElevationProfile().getWithEle(getOutlineXZ(from, to));
		
	}
	
	@Override
	public SimplePolygonXZ getOutlinePolygonXZ() {
		List<VectorXZ> outlineXZ = new ArrayList<VectorXZ>(getOutlineXZ(0, 0));
		outlineXZ.addAll(getOutlineXZ(1, 1));
		outlineXZ.add(outlineXZ.get(0));
		return new SimplePolygonXZ(outlineXZ);
	}

	@Override
	public PolygonXYZ getOutlinePolygon() {
		List<VectorXYZ> outline = new ArrayList<VectorXYZ>(getOutline(0, 0));
		outline.addAll(getOutline(1, 1));
		outline.add(outline.get(0));
		return new PolygonXYZ(outline);
	}

	/**
	 * throws an IllegalStateException if information hasn't been
	 * provided by a {@link NetworkCalculator}
	 */
	private void checkInformationProvided() throws IllegalStateException {
		if (!informationProvided) {
			throw new IllegalStateException("no connector information"
					+ " has been set for this representation.\n"
					+ "node: " + node);
		}
	}
	
	@Override
	public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
		return new AxisAlignedBoundingBoxXZ(getOutlinePolygon().getVertices());
	}
		
	@Override
	public String toString() {
		return "visible connector node WO for " + node;
	}
	
}
