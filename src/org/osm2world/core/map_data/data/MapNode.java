package org.osm2world.core.map_data.data;

import static org.osm2world.core.math.VectorXZ.X_UNIT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_elevation.data.NodeElevationProfile;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.osm.data.OSMNode;
import org.osm2world.core.world.data.NodeWorldObject;

/**
 * grid representation of an OSM node,
 * references inbound and outbound {@link MapWaySegment}s.
 * For each OSM node, one GridNode will be created.
 */
public class MapNode implements MapElement {

	private final VectorXZ pos;
	private final OSMNode osmNode;
	
	private List<NodeWorldObject> representations = new ArrayList<NodeWorldObject>(1);
	private NodeElevationProfile elevationProfile;
	
	private List<MapWaySegment> connectedWaySegments = new ArrayList<MapWaySegment>();
	private List<MapSegment> connectedSegments = new ArrayList<MapSegment>();
	
	private List<MapWaySegment> inboundLines = new ArrayList<MapWaySegment>(); //TODO: maybe use list and sort by angle?
	private List<MapWaySegment> outboundLines = new ArrayList<MapWaySegment>();
		
	private Collection<MapArea> adjacentAreas;
	
	public MapNode(VectorXZ pos, OSMNode osmNode) {
		
		this.pos = pos;
		this.osmNode = osmNode;
		this.adjacentAreas = new ArrayList<MapArea>();
		
	}
	
	public VectorXZ getPos() {
		return pos;
	}
	
	@Override
	public int getLayer() {
		if (osmNode.tags.containsKey("layer")) {
			try {
				return Integer.parseInt(osmNode.tags.getValue("layer"));
			} catch (NumberFormatException nfe) {
				return 0;
			}
		}
		return 0;
	}
	
	public OSMNode getOsmNode() {
		return osmNode;
	}
	
	@Override
	public TagGroup getTags() {
		return getOsmNode().tags;
	}
	
	public Collection<MapArea> getAdjacentAreas() {
		return adjacentAreas;
	}
	
	public void addInboundLine(MapWaySegment inboundLine) {
		
		connectedWaySegments.add(inboundLine);
		connectedSegments.add(inboundLine);
		inboundLines.add(inboundLine);

		sortLinesByAngle(connectedWaySegments);
		sortLinesByAngle(connectedSegments);
		sortLinesByAngle(inboundLines);
		
	}
	
	public void addOutboundLine(MapWaySegment outboundLine) {
		
		connectedWaySegments.add(outboundLine);
		connectedSegments.add(outboundLine);
		outboundLines.add(outboundLine);
		
		sortLinesByAngle(connectedWaySegments);
		sortLinesByAngle(connectedSegments);
		sortLinesByAngle(outboundLines);
		
	}
	
	/**
	 * returns those connected lines that end here.
	 * Sorting is as for {@link #getConnectedWaySegments()}.
	 */
	public List<MapWaySegment> getInboundLines() {
		return inboundLines;
	}
	
	/**
	 * returns those connected lines that start here.
	 * Sorting is as for {@link #getConnectedWaySegments()}.
	 */
	public List<MapWaySegment> getOutboundLines() {
		return outboundLines;
	}
	
	public void addAdjacentArea(MapArea adjacentArea) {
		adjacentAreas.add(adjacentArea);
	}

	//TODO: with all that "needs to be called before x" etc. stuff (also in MapArea), switch to BUILDER?
	/** needs to be called after adding and completing all adjacent areas */
	public void calculateAdjacentAreaSegments() {
	
		for (MapArea adjacentArea : adjacentAreas) {
			for (MapAreaSegment areaSegment : adjacentArea.getAreaSegments()) {
				if (areaSegment.getStartNode() == this
						|| areaSegment.getEndNode() == this) {
					connectedSegments.add(areaSegment);
				}
			}
		}

		sortLinesByAngle(connectedSegments);
	}
		
	/**
	 * returns all way segments connected with this node.
	 * They will be sorted according to the clockwise
	 * (seen from above) angle between the vector
	 * "this node -> other node of the segment"
	 * and the positive x direction.
	 */
	public List<MapWaySegment> getConnectedWaySegments() {
		return connectedWaySegments;
	}
	
	/**
	 * returns all way segments and area segments connected with this node.
	 * Sorted like {@link #getConnectedWaySegments()}.
	 */
	public List<MapSegment> getConnectedSegments() {
		return connectedSegments;
	}
	
	/**
	 * creates the ordering described for {@link #getConnectedSegments()}
	 */
	private void sortLinesByAngle(List<? extends MapSegment> lines) {
		Collections.sort(lines, new Comparator<MapSegment>() {
			@Override
			public int compare(MapSegment l1, MapSegment l2) {
				
				VectorXZ d1 = l1.getDirection();
				VectorXZ d2 = l2.getDirection();
				
				if (inboundLines.contains(l1)) {
					d1 = d1.invert();
				}
				if (inboundLines.contains(l2)) {
					d2 = d2.invert();
				}
				
				//check whether the lines are in the first or second 180°
				//(the dot product formula will not be useful to distinguish
				//these cases - it only provides cos 0° to cos 180° - ,
				//so they need to be handled first)
				
				if (d1.z < 0 && d2.z > 0) {
					return -1;
				} else if (d1.z > 0 && d2.z < 0) {
					return +1;
				}
				
				//check the actual angles using the dot product with (1,0,0).
				//Two simplifications apply:
				//- we don't need to divide by the vector lengths' product,
				//  as getDirection returns vectors whose length is 1
				//- we don't need to actually calculate the angle itself,
				//  because if angle a > angle b, then cos a < cos b
				//  (in [0°, 180°])
				
				double comparison = d1.dot(X_UNIT) - d2.dot(X_UNIT);
				
				if (comparison == 0) {
					return 0;
				}
				
				if (d1.z < 0) { //and d2.z < 0
					return (comparison > 0) ? -1 : +1;
				} else { //d1.z > 0 and d2.z > 0
					return (comparison > 0) ? +1 : -1;
				}
				
			}
		});
	}
	
	@Override
	public List<NodeWorldObject> getRepresentations() {
		return representations;
	}
	
	@Override
	public NodeWorldObject getPrimaryRepresentation() {
		if (representations.isEmpty()) {
			return null;
		} else {
			return representations.get(0);
		}
	}

	/**
	 * adds a visual representation for this node
	 */
	public void addRepresentation(NodeWorldObject representation) {
		this.representations.add(representation);
	}
	
	@Override
	public NodeElevationProfile getElevationProfile() {
		return elevationProfile;
	}

	public void setElevationProfile(NodeElevationProfile elevationProfile) {
		this.elevationProfile = elevationProfile;
	}
	
	@Override
	public String toString() {
		return osmNode.toString();
	}

	@Override
	public Collection<MapOverlap<?,?>> getOverlaps() {
		return Collections.emptyList();
	}

	@Override
	public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
		return new AxisAlignedBoundingBoxXZ(pos.x, pos.z, pos.x, pos.z);
	}
	
}
