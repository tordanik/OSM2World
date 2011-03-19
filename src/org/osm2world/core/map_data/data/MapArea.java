package org.osm2world.core.map_data.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_elevation.data.AreaElevationProfile;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.osm.data.OSMElement;
import org.osm2world.core.osm.data.OSMWay;
import org.osm2world.core.world.data.AreaWorldObject;

public class MapArea implements MapElement {

	private final List<MapNode> nodes;

	private final OSMElement objectWithTags;
	private final OSMWay osmWay;
	
	private List<List<MapNode>> holes = Collections.emptyList();
	
	private PolygonWithHolesXZ polygon;
	
	private Collection<MapAreaSegment> areaSegments;

	@SuppressWarnings("unchecked") //is later checked for EMPTY_LIST using ==
	private Collection<MapOverlap<?,?>> overlaps = Collections.EMPTY_LIST;
	
	private List<AreaWorldObject> representations =
		new ArrayList<AreaWorldObject>(1);
	private AreaElevationProfile elevationProfile;
	
	//TODO: contained / intersecting nodes/lines
	
	public MapArea(OSMElement objectWithTags, OSMWay osmWay) {
		
		this.objectWithTags = objectWithTags;
		this.osmWay = osmWay;
		this.nodes = new ArrayList<MapNode>();
		
	}

	public MapArea(OSMWay osmWay) {
		this(osmWay, osmWay);
	}
	
	public void addBoundaryNode(MapNode boundaryNode) {
		nodes.add(boundaryNode);
	}

	public List<MapNode> getBoundaryNodes() {
		return nodes;
	}
	
	@Override
	public int getLayer() {
		if (osmWay.tags.containsKey("layer")) {
			try {
				return Integer.parseInt(osmWay.tags.getValue("layer"));
			} catch (NumberFormatException nfe) {
				return 0;
			}
		}
		return 0;
	}

	public void setHoles(Collection<List<MapNode>> holes) {
		this.holes = new ArrayList<List<MapNode>>(holes);
	}
	
	public Collection<List<MapNode>> getHoles() {
		return holes;
	}
	
	/** returns the way or relation with the tags for this area */
	public OSMElement getOsmObject() {
		return objectWithTags;
	}
	
	/**
	 * returns the way that is the outer boundary of this object.
	 * Not necessarily the same as {@link #getOsmObject()} (because of multipolygons)!
	 */
	public OSMWay getOsmWay() {
		return osmWay;
	}

	@Override
	public TagGroup getTags() {
		return objectWithTags.tags;
	}
	
	/**
	 * returns the area's polygon.
	 * Must not be called before all boundary nodes have been added!
	 */
	public PolygonWithHolesXZ getPolygon() {
		
		if (polygon == null) {
			
			SimplePolygonXZ outerPolygon = polygonFromGridNodeSequence(nodes);
			
			List<SimplePolygonXZ> holePolygons =
				new ArrayList<SimplePolygonXZ>(holes.size());
			for (List<MapNode> hole : holes) {
				holePolygons.add(polygonFromGridNodeSequence(hole));
			}
			
			polygon = new PolygonWithHolesXZ(outerPolygon, holePolygons);
			
		}
		
		return polygon;
	}

	public SimplePolygonXZ getOuterPolygon() {
		return polygon.getOuter();
	}

	private static SimplePolygonXZ polygonFromGridNodeSequence(
			List<MapNode> nodes) {
		
		List<VectorXZ> vertices = new ArrayList<VectorXZ>(nodes.size());
		
		for (MapNode node : nodes) {
			vertices.add(node.getPos());
		}
		
		return new SimplePolygonXZ(vertices);
		
	}
	
	/**
	 * returns the segments making up this area's outer and inner boundaries
	 */
	public Collection<MapAreaSegment> getAreaSegments() {
		
		if (areaSegments == null) {
		
			areaSegments = new ArrayList<MapAreaSegment>();
			
			for (int v = 0; v+1 < nodes.size(); v++) {
				//relies on duplication of first/last node
				
				areaSegments.add(new MapAreaSegment(this,
						getPolygon().getOuter().isClockwise(),
						nodes.get(v), nodes.get(v+1)));
								
			}
			
			for (int h = 0; h < holes.size(); h++) {
				
				List<MapNode> holeNodes = holes.get(h);
				SimplePolygonXZ holePolygon = polygon.getHoles().get(h);
				
				for (int v = 0; v+1 < holeNodes.size(); v++) {
					//relies on duplication of first/last node
					
					areaSegments.add(new MapAreaSegment(this,
							!holePolygon.isClockwise(),
							holeNodes.get(v), holeNodes.get(v+1)));
					
				}
				
			}
			
		}
		
		return areaSegments;
		
	}
	
	@Override
	public List<AreaWorldObject> getRepresentations() {
		return representations;
	}
	
	@Override
	public AreaWorldObject getPrimaryRepresentation() {
		if (representations.isEmpty()) {
			return null;
		} else {
			return representations.get(0);
		}
	}

	/**
	 * adds a visual representation for this area
	 */
	public void addRepresentation(AreaWorldObject representation) {
		this.representations.add(representation);
	}
	
	@Override
	public AreaElevationProfile getElevationProfile() {
		return elevationProfile;
	}
	
	public void setElevationProfile(AreaElevationProfile elevationProfile) {
		this.elevationProfile = elevationProfile;
	}
	
	public void addOverlap(MapOverlap<?, ?> overlap) {
		assert overlap.e1 == this || overlap.e2 == this;
		if (overlaps == Collections.EMPTY_LIST) {
			overlaps = new ArrayList<MapOverlap<?,?>>();
		}
		overlaps.add(overlap);
	}

	@Override
	public Collection<MapOverlap<?,?>> getOverlaps() {
		return overlaps;
	}
	
	@Override
	public String toString() {
		return objectWithTags.toString();
	}
	
}
