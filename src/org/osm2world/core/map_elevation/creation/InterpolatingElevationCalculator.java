package org.osm2world.core.map_elevation.creation;

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.time.StopWatch;
import org.osm2world.EleInterpolationStrategy;
import org.osm2world.Hardcoded;
import org.osm2world.LeastSquaresStrategy;
import org.osm2world.SRTMData;
import org.osm2world.core.heightmap.data.CellularTerrainElevation;
import org.osm2world.core.heightmap.data.TerrainPoint;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.AreaElevationProfile;
import org.osm2world.core.map_elevation.data.NodeElevationProfile;
import org.osm2world.core.map_elevation.data.WaySegmentElevationProfile;
import org.osm2world.core.math.VectorXYZ;

/**
 * sets elevations based on an {@link EleInterpolationStrategy}
 */
public class InterpolatingElevationCalculator implements ElevationCalculator {

	protected EleInterpolationStrategy buildStrategy() {
		return new LeastSquaresStrategy();
		//return new InverseDistanceWeightingStrategy(1.5);
	}
	
	private final MapProjection mapProjection;
	
	public InterpolatingElevationCalculator(MapProjection mapProjection) {
		this.mapProjection = mapProjection;
	}
	
	@Override
	public void calculateElevations(MapData mapData, CellularTerrainElevation eleData) {

		Collection<VectorXYZ> sites = emptyList();
		
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		
		try {
			
			SRTMData srtmData = new SRTMData(Hardcoded.SRTM_DIR, mapProjection);
			
			System.out.println("time srtm: " + stopWatch);
			stopWatch.reset();
			stopWatch.start();
			
			sites = srtmData.getSites(Hardcoded.SRTM_minLon, Hardcoded.SRTM_minLat,
					Hardcoded.SRTM_maxLon, Hardcoded.SRTM_maxLat);
			
			System.out.println("time getSites: " + stopWatch);
			stopWatch.reset();
			stopWatch.start();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		EleInterpolationStrategy strategy = buildStrategy();
		strategy.setKnownSites(sites);
		
		System.out.println("time setKnownSites: " + stopWatch);
		stopWatch.reset();
		stopWatch.start();
		
		/* set nodes' elevation profiles */
		
		for (MapNode node : mapData.getMapNodes()) {
			
			double ele = strategy.interpolateEle(node.getPos()).y;
			NodeElevationProfile profile = new NodeElevationProfile(node);
			profile.setEle(ele);
			node.setElevationProfile(profile);
			
		}

		System.out.println("time node ele: " + stopWatch);
		stopWatch.reset();
		stopWatch.start();
		
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
		
		if (eleData != null) {
			for (TerrainPoint point : eleData.getTerrainPoints()) {
				double ele = strategy.interpolateEle(point.getPos()).y;
				point.setEle((float)(double)ele);
			}
		}
		
	}
	
}
