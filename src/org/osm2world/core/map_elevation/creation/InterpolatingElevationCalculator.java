package org.osm2world.core.map_elevation.creation;

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.lang.time.StopWatch;
import org.osm2world.EleInterpolationStrategy;
import org.osm2world.LeastSquaresStrategy;
import org.osm2world.TerrainElevationData;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.util.FaultTolerantIterationUtil;
import org.osm2world.core.util.FaultTolerantIterationUtil.Operation;
import org.osm2world.core.world.data.WorldObject;

/**
 * sets elevations based on an {@link EleInterpolationStrategy}
 */
public class InterpolatingElevationCalculator implements ElevationCalculator {

	protected EleInterpolationStrategy buildStrategy() {
		return new LeastSquaresStrategy();
		//return new InverseDistanceWeightingStrategy(1.5);
	}
	
	@Override
	public void calculateElevations(MapData mapData, TerrainElevationData eleData) {
	
		Collection<VectorXYZ> sites = emptyList();
		
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		
		try {
						
			System.out.println("time srtm: " + stopWatch);
			stopWatch.reset();
			stopWatch.start();
						
			sites = eleData.getSites(mapData);
			
			System.out.println("time getSites: " + stopWatch);
			stopWatch.reset();
			stopWatch.start();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		final EleInterpolationStrategy strategy = buildStrategy();
		strategy.setKnownSites(sites);
		
		System.out.println("time setKnownSites: " + stopWatch);
		stopWatch.reset();
		stopWatch.start();
		
		// TODO: collect & connect connectors
		
		/* set connectors' elevations */
		
		for (MapElement element : mapData.getMapElements()) {
			
			FaultTolerantIterationUtil.iterate(element.getRepresentations(),
					new Operation<WorldObject>() {
				@Override public void perform(WorldObject worldObject) {
					
					for (EleConnector conn : worldObject.getEleConnectors()) {
						conn.setPosXYZ(strategy.interpolateEle(conn.pos));
					}
					
				}
			});
			
		}

		System.out.println("time node ele: " + stopWatch);
		stopWatch.reset();
		stopWatch.start();
		
	}
	
}
