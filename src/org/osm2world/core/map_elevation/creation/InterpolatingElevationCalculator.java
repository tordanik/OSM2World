package org.osm2world.core.map_elevation.creation;

import static java.util.Collections.emptyList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.time.StopWatch;
import org.osm2world.EleInterpolationStrategy;
import org.osm2world.LeastSquaresStrategy;
import org.osm2world.TerrainElevationData;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.JoinedEleConnectors;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.util.FaultTolerantIterationUtil;
import org.osm2world.core.util.FaultTolerantIterationUtil.Operation;
import org.osm2world.core.world.data.WorldObject;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

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
		
		/* interpolate connectors' elevations */
		
		final List<EleConnector> connectors = new ArrayList<EleConnector>();
		
		for (MapElement element : mapData.getMapElements()) {
			
			FaultTolerantIterationUtil.iterate(element.getRepresentations(),
					new Operation<WorldObject>() {
				@Override public void perform(WorldObject worldObject) {
					
					for (EleConnector conn : worldObject.getEleConnectors()) {
						conn.setPosXYZ(strategy.interpolateEle(conn.pos));
						connectors.add(conn);
					}
					
				}
			});
			
		}

		System.out.println("time terrain interpolation: " + stopWatch);
		stopWatch.reset();
		stopWatch.start();
		
		/* enforce constraints using LP */
		
		final LPEleConstraintEnforcer enforcer = new LPEleConstraintEnforcer();
		
		enforcer.addConnectors(joinConnectors(connectors));
		
		/* TOOD: maybe also treat constant vertical distance as
		 * member of the same group, then assign eles internally? */
			
		//add constraints defined by WorldObjects
		
		for (MapElement element : mapData.getMapElements()) {
			
			FaultTolerantIterationUtil.iterate(element.getRepresentations(),
					new Operation<WorldObject>() {
				@Override public void perform(WorldObject worldObject) {
					
					worldObject.addEleConstraints(enforcer);
					
				}
			});
			
		}
		
		System.out.println("time add constraints: " + stopWatch);
		stopWatch.reset();
		stopWatch.start();
		
		enforcer.enforceConstraints();
		
		System.out.println("time enforce constraints: " + stopWatch);
		stopWatch.reset();
		stopWatch.start();
		
	}

	/**
	 * join connected connectors by constraining them to the same elevation
	 */
	private Collection<JoinedEleConnectors> joinConnectors(
			Iterable<EleConnector> connectors) {
		
		Multimap<VectorXZ, JoinedEleConnectors> connectorJoinMap =
				HashMultimap.create();
		
		connectors:
		for (EleConnector c : connectors) {
			
			Collection<JoinedEleConnectors> existingJoinedConns =
					connectorJoinMap.get(c.pos);
			
			if (existingJoinedConns != null){
				for (JoinedEleConnectors cs : existingJoinedConns) {
					if (cs.connectsTo(c)) {
						cs.add(c);
						continue connectors;
					}
				}
			}
			
			// add new entry if no existing set of joined connectors matched c
			connectorJoinMap.put(c.pos, new JoinedEleConnectors(c));
			
		}
		
		return connectorJoinMap.values();
		
	}
	
}
