package org.osm2world.core.map_elevation.creation;

import java.util.List;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.AreaElevationProfile;
import org.osm2world.core.map_elevation.data.NodeElevationProfile;
import org.osm2world.core.map_elevation.data.WaySegmentElevationProfile;

/**
 * assigns an elevation of 0 to everything.
 * Useful for certain use cases, e.g. fast creation of tiled pseudo-3D tiles.
 */
public class ZeroElevationCalculator implements ElevationCalculator {

	@Override
	public void calculateElevations(MapData mapData,
			TerrainElevationData eleData) {
				
		for (MapNode node : mapData.getMapNodes()) {
							
			NodeElevationProfile profile = new NodeElevationProfile(node);
			profile.setEle(0);
			//TODO replace old ElevationProfile stuff
//			node.setElevationProfile(profile);
						
		}
		
		for (MapWaySegment segment : mapData.getMapWaySegments()) {

			if (segment.getPrimaryRepresentation() == null) continue;
			
			WaySegmentElevationProfile profile =
				new WaySegmentElevationProfile(segment);
			
			//TODO replace old ElevationProfile stuff
//			profile.addPointWithEle(
//				segment.getStartNode().getElevationProfile().getPointWithEle());
//			profile.addPointWithEle(
//				segment.getEndNode().getElevationProfile().getPointWithEle());
//
//			segment.setElevationProfile(profile);
			
		}
		
		/* set areas' elevation profiles (based on nodes' elevations) */
		
		for (MapArea area : mapData.getMapAreas()) {
			
			if (area.getPrimaryRepresentation() == null) continue;
			
			AreaElevationProfile profile =
				new AreaElevationProfile(area);
			
			for (MapNode node : area.getBoundaryNodes()) {
				//TODO replace old ElevationProfile stuff
//				profile.addPointWithEle(
//					node.getElevationProfile().getPointWithEle());
			}
			
			for (List<MapNode> holeOutline : area.getHoles()) {
				for (MapNode node : holeOutline) {
					//TODO replace old ElevationProfile stuff
//					profile.addPointWithEle(
//						node.getElevationProfile().getPointWithEle());
				}
			}
			
			//TODO replace old ElevationProfile stuff
//			area.setElevationProfile(profile);
			
		}
		
	}
	
}
