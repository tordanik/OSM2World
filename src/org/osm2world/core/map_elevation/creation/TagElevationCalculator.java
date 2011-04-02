package org.osm2world.core.map_elevation.creation;

import java.util.List;

import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;
import org.osm2world.core.heightmap.data.CellularTerrainElevation;
import org.osm2world.core.heightmap.data.TerrainPoint;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapAreaSegment;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapSegment;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.AreaElevationProfile;
import org.osm2world.core.map_elevation.data.NodeElevationProfile;
import org.osm2world.core.map_elevation.data.WaySegmentElevationProfile;

/**
 * relies on tags that explicitly set elevation.
 * Subclasses determine the tag(s) to be used for this purpose.
 */
public abstract class TagElevationCalculator implements ElevationCalculator {
	
	Double terrainElevation;
	
	/**
	 * @param terrainElevation  elevation for the terrain
	 */
	public TagElevationCalculator(Double terrainElevation) {
		this.terrainElevation = terrainElevation;
	}
	
	public TagElevationCalculator() {
		this(0.0);
	}
	
	@Override
	public void calculateElevations(MapData mapData,
			CellularTerrainElevation eleData) {
		
		/* set nodes' elevation profiles */
		
		for (MapNode node : mapData.getMapNodes()) {
			
			Double ele = getEleForTags(node.getTags());
			
			if (ele == null) {
				
				/* use elevation information from nodes or areas containing
				 * this node. If they have contradicting information, the
				 * results will be unpredictable.
				 */
				
				for (MapSegment segment : node.getConnectedSegments()) {
					TagGroup tags;
					if (segment instanceof MapWaySegment) {
						tags = ((MapWaySegment) segment).getTags();
					} else {
						tags = ((MapAreaSegment) segment).getArea().getTags();
					}
					ele = getEleForTags(tags);
				}
				
			}
			
			if (ele == null) {
				System.err.println("node without ele information: " + node);
				ele = 0.0;
			}
			
			NodeElevationProfile profile = new NodeElevationProfile(node);
			profile.setEle(ele);
			node.setElevationProfile(profile);
			
		}
		
		/* set way segments' elevation profiles (based on nodes' elevations) */
		
		for (MapWaySegment segment : mapData.getMapWaySegments()) {

			if (segment.getPrimaryRepresentation() == null) continue;
			
			WaySegmentElevationProfile profile =
				new WaySegmentElevationProfile(segment);
			
			profile.addPointWithEle(
				segment.getStartNode().getElevationProfile().getPointWithEle());
			profile.addPointWithEle(
				segment.getEndNode().getElevationProfile().getPointWithEle());
			
			segment.setElevationProfile(profile);
			
		}
		
		/* set areas' elevation profiles (based on nodes' elevations) */
		
		for (MapArea area : mapData.getMapAreas()) {
			
			if (area.getPrimaryRepresentation() == null) continue;
			
			AreaElevationProfile profile =
				new AreaElevationProfile(area);
			
			for (MapNode node : area.getBoundaryNodes()) {
				profile.addPointWithEle(
					node.getElevationProfile().getPointWithEle());
			}
			
			for (List<MapNode> holeOutline : area.getHoles()) {
				for (MapNode node : holeOutline) {
					profile.addPointWithEle(
						node.getElevationProfile().getPointWithEle());
				}
			}
			
			area.setElevationProfile(profile);
			
		}
		
		/* set terrain elevation */
		
		for (TerrainPoint point : eleData.getTerrainPoints()) {
			point.setEle((float)(double)terrainElevation);
		}
		
	}
	
	/**
	 * returns the elevation as set explicitly by the tags
	 * 
	 * @return  elevation; null if the tags don't define the elevation
	 */
	protected abstract Double getEleForTags(TagGroup tags);

}
