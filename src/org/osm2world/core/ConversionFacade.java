package org.osm2world.core;

import static java.util.Collections.emptyList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.time.StopWatch;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.osm2world.TerrainInterpolator;
import org.osm2world.Hardcoded;
import org.osm2world.LeastSquaresInterpolator;
import org.osm2world.SRTMData;
import org.osm2world.TerrainElevationData;
import org.osm2world.core.map_data.creation.HackMapProjection;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.map_data.creation.OSMToMapDataConverter;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.EleConstraintValidator;
import org.osm2world.core.map_elevation.creation.NoneEleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.osm.creation.JOSMFileHack;
import org.osm2world.core.osm.creation.OsmosisReader;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.terrain.data.Terrain;
import org.osm2world.core.util.FaultTolerantIterationUtil;
import org.osm2world.core.util.FaultTolerantIterationUtil.Operation;
import org.osm2world.core.util.functions.DefaultFactory;
import org.osm2world.core.util.functions.Factory;
import org.osm2world.core.world.creation.WorldCreator;
import org.osm2world.core.world.creation.WorldModule;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.modules.BarrierModule;
import org.osm2world.core.world.modules.BridgeModule;
import org.osm2world.core.world.modules.BuildingModule;
import org.osm2world.core.world.modules.CliffModule;
import org.osm2world.core.world.modules.InvisibleModule;
import org.osm2world.core.world.modules.ParkingModule;
import org.osm2world.core.world.modules.PoolModule;
import org.osm2world.core.world.modules.PowerModule;
import org.osm2world.core.world.modules.RailwayModule;
import org.osm2world.core.world.modules.RoadModule;
import org.osm2world.core.world.modules.StreetFurnitureModule;
import org.osm2world.core.world.modules.SurfaceAreaModule;
import org.osm2world.core.world.modules.TreeModule;
import org.osm2world.core.world.modules.TunnelModule;
import org.osm2world.core.world.modules.WaterModule;

/**
 * provides an easy way to call all steps of the conversion process
 * in the correct order
 */
public class ConversionFacade {
	
	/**
	 * all results of a conversion run
	 */
	public static final class Results {
		
		private final MapProjection mapProjection;
		private final MapData map;
		private final Terrain terrain;
		private final TerrainElevationData eleData;
		
		public Results(MapProjection mapProjection, MapData grid,
				Terrain terrain, TerrainElevationData eleData) {
			this.mapProjection = mapProjection;
			this.map = grid;
			this.terrain = terrain;
			this.eleData = eleData;
		}
		
		public MapProjection getMapProjection() {
			return mapProjection;
		}
		
		public MapData getMapData() {
			return map;
		}
		
		/**
		 * returns the terrain. Will be null if terrain creation was disabled.
		 */
		public Terrain getTerrain() {
			return terrain;
		}
		
		/*
		 * TODO: remove in the future, because it isn't really a conversion result.
		 * Once real elevation data is available, this should be a conversion *input*
		 */
		public TerrainElevationData getEleData() {
			return eleData;
		}
		
		/**
		 * collects and returns all representations that implement a
		 * renderableType, including terrain.
		 * Convenience method.
		 */
		public <R extends Renderable> Collection<R> getRenderables(Class<R> renderableType) {
			return getRenderables(renderableType, true, true);
		}
		
		/**
		 * @see #getRenderables(Class)
		 */
		public <R extends Renderable> Collection<R> getRenderables(
				Class<R> renderableType, boolean includeGrid, boolean includeTerrain) {
			
			Collection<R> representations = new ArrayList<R>();
			
			if (includeGrid) {
				for (R r : map.getWorldObjects(renderableType)) {
					representations.add(r);
				}
			}
			
			if (includeTerrain && terrain != null &&
					renderableType.isAssignableFrom(Terrain.class)) {
				@SuppressWarnings("unchecked") //checked by isAssignableFromd
				R renderable = (R) terrain;
				representations.add(renderable);
			}
			
			return representations;
			
		}
		
	}
	
	/**
	 * generates a default list of modules for the conversion
	 */
	private static final List<WorldModule> createDefaultModuleList() {
		
		return Arrays.asList((WorldModule)
				new RoadModule(),
				new RailwayModule(),
				new BuildingModule(),
				new ParkingModule(),
				new TreeModule(),
				new StreetFurnitureModule(),
				new WaterModule(),
				new PoolModule(),
				new CliffModule(),
				new BarrierModule(),
				new PowerModule(),
				new BridgeModule(),
				new TunnelModule(),
				new SurfaceAreaModule(),
				new InvisibleModule()
		);
		
	}
	
	private Factory<? extends TerrainInterpolator> terrainEleInterpolatorFactory =
		new DefaultFactory<LeastSquaresInterpolator>(LeastSquaresInterpolator.class);

	private Factory<? extends EleConstraintEnforcer> eleConstraintEnforcerFactory =
		new DefaultFactory<NoneEleConstraintEnforcer>(NoneEleConstraintEnforcer.class);
	
	/**
	 * sets the factory that will make {@link EleConstraintEnforcer}
	 * instances during subsequent calls to
	 * {@link #createRepresentations(OSMData, List, Configuration, List)}.
	 * 
	 * @see DefaultFactory
	 */
	public void setEleConstraintEnforcerFactory(
			Factory<? extends EleConstraintEnforcer> interpolatorFactory) {
		this.eleConstraintEnforcerFactory = interpolatorFactory;
	}
	
	/**
	 * sets the factory that will make {@link TerrainInterpolator}
	 * instances during subsequent calls to
	 * {@link #createRepresentations(OSMData, List, Configuration, List)}.
	 * 
	 * @see DefaultFactory
	 */
	public void setTerrainEleInterpolatorFactory(
			Factory<? extends TerrainInterpolator> enforcerFactory) {
		this.terrainEleInterpolatorFactory = enforcerFactory;
	}
	
	
	/**
	 * performs all necessary steps to go from
	 * an OSM file to the renderable {@link WorldObject}s.
	 * Sends updates to {@link ProgressListener}s.
	 * 
	 * @param osmFile       file to read OSM data from; != null
	 * @param worldModules  modules that will create the {@link WorldObject}s
	 *                      in the result; null to use a default module list
	 * @param config        set of parameters that controls various aspects
	 *                      of the modules' behavior; null to use defaults
	 * @param targets       receivers of the conversion results; can be null if
	 *                      you want to handle the returned results yourself
	 */
	public Results createRepresentations(File osmFile,
			List<WorldModule> worldModules, Configuration config,
			List<Target<?>> targets)
			throws IOException {
		
		if (osmFile == null) {
			throw new IllegalArgumentException("osmFile must not be null");
		}
		
		OSMData osmData = null;
		boolean useJOSMHack = false;
		
		if (JOSMFileHack.isJOSMGenerated(osmFile)) {
			useJOSMHack = true;
		} else {
			
			/* try to read file using Osmosis */
			
			try {
				osmData = new OsmosisReader(osmFile).getData();
			} catch (IOException e) {
				
				System.out.println("could not read file," +
						" trying workaround for files created by JOSM");
				
				useJOSMHack = true;
							
			}
			
		}
		
		/* create a temporary "cleaned up" file as workaround for JOSM files */
		
		if (useJOSMHack) {
			
			File tempFile;
			try {
				tempFile = JOSMFileHack.createTempOSMFile(osmFile);
			} catch (Exception e2) {
				throw new IOException("could not read OSM file" +
						" (not even with workaround for JOSM files)", e2);
			}
			
			osmData = new OsmosisReader(tempFile).getData();
			
		}
		
		return createRepresentations(osmData, worldModules, config, targets);
		
	}
	
	
	/**
	 * variant of
	 * {@link #createRepresentations(File, List, Configuration, List)}
	 * that accepts {@link OSMData} instead of a file.
	 * Use this when all data is already
	 * in memory, for example with editor applications.
	 * 
	 * @param osmData       input data; != null
	 * @param worldModules  modules that will create the {@link WorldObject}s
	 *                      in the result; null to use a default module list
	 * @param config        set of parameters that controls various aspects
	 *                      of the modules' behavior; null to use defaults
	 * @param targets       receivers of the conversion results; can be null if
	 *                      you want to handle the returned results yourself
	 * 
	 * @throws BoundingBoxSizeException  for oversized bounding boxes
	 */
	public Results createRepresentations(OSMData osmData,
			List<WorldModule> worldModules, Configuration config,
			List<Target<?>> targets)
			throws IOException, BoundingBoxSizeException {
		
		/* check the inputs */
		
		if (osmData == null) {
			throw new IllegalArgumentException("osmData must not be null");
		}
		
		if (config == null) {
			config = new BaseConfiguration();
		}
		
		Double maxBoundingBoxDegrees = config.getDouble("maxBoundingBoxDegrees", null);
		if (maxBoundingBoxDegrees != null) {
			for (Bound bound : osmData.getBounds()) {
				if (bound.getTop() - bound.getBottom() > maxBoundingBoxDegrees
						|| bound.getRight() - bound.getLeft() > maxBoundingBoxDegrees) {
					throw new BoundingBoxSizeException(bound);
				}
			}
		}
		
		/* create map data from OSM data */
		updatePhase(Phase.MAP_DATA);
		
		MapProjection mapProjection = new HackMapProjection(osmData);
		OSMToMapDataConverter converter = new OSMToMapDataConverter(mapProjection);
		MapData mapData = converter.createMapData(osmData);
		
		/* apply world modules */
		updatePhase(Phase.REPRESENTATION);
		
		if (worldModules == null) {
			worldModules = createDefaultModuleList();
		}
		
		Materials.configureMaterials(config);
			//this will cause problems if multiple conversions are run
			//at the same time, because global variables are being modified
		
		WorldCreator moduleManager =
			new WorldCreator(config, worldModules);
		moduleManager.addRepresentationsTo(mapData);
		
		/* determine elevations */
		updatePhase(Phase.ELEVATION);
		
		TerrainElevationData eleData = null;
		if (config.getBoolean("createTerrain", true)) {
			eleData = new SRTMData(Hardcoded.SRTM_DIR, mapProjection);
		}
		
		calculateElevations(mapData, eleData, config);
		
		/* create terrain */
		updatePhase(Phase.TERRAIN);
		
		Terrain terrain = null;
		
		/* supply results to targets and caller */
		updatePhase(Phase.FINISHED);
		
		boolean underground = config.getBoolean("renderUnderground", true);
		
		if (targets != null) {
			for (Target<?> target : targets) {
				TargetUtil.renderWorldObjects(target, mapData, underground);
				if (terrain != null) {
					TargetUtil.renderObject(target, terrain);
				}
				target.finish();
			}
		}
		
		return new Results(mapProjection, mapData, terrain, eleData);
		
	}
	
	/**
	 * uses OSM data and an terrain elevation data (usually from an external
	 * source) to calculate elevations for all {@link EleConnector}s of the
	 * {@link WorldObject}s
	 */
	private void calculateElevations(MapData mapData,
			TerrainElevationData eleData, Configuration config) {
		
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
		
		final TerrainInterpolator strategy =
				terrainEleInterpolatorFactory.make();
		
		strategy.setKnownSites(sites);
		
		System.out.println("time setKnownSites: " + stopWatch);
		stopWatch.reset();
		stopWatch.start();
		
		/* interpolate connectors' elevations */
		
		final List<EleConnector> connectors = new ArrayList<EleConnector>();
		
		FaultTolerantIterationUtil.iterate(mapData.getWorldObjects(),
				new Operation<WorldObject>() {
			@Override public void perform(WorldObject worldObject) {
				
				for (EleConnector conn : worldObject.getEleConnectors()) {
					conn.setPosXYZ(strategy.interpolateEle(conn.pos));
					connectors.add(conn);
				}
				
			}
		});
		
		System.out.println("time terrain interpolation: " + stopWatch);
		stopWatch.reset();
		stopWatch.start();
		
		/* enforce constraints defined by WorldObjects */
		
		boolean debugConstraints = config.getBoolean("debugConstraints", false);
		
		final EleConstraintEnforcer enforcer = debugConstraints
				? new EleConstraintValidator(mapData,
						eleConstraintEnforcerFactory.make())
				: eleConstraintEnforcerFactory.make();
		
		enforcer.addConnectors(connectors);
		
		FaultTolerantIterationUtil.iterate(mapData.getWorldObjects(),
				new Operation<WorldObject>() {
			@Override public void perform(WorldObject worldObject) {
				
				worldObject.defineEleConstraints(enforcer);
				
			}
		});
		
		System.out.println("time add constraints: " + stopWatch);
		stopWatch.reset();
		stopWatch.start();
		
		enforcer.enforceConstraints();
		
		System.out.println("time enforce constraints: " + stopWatch);
		stopWatch.reset();
		stopWatch.start();
		
	}
	
	public static enum Phase {
		MAP_DATA,
		REPRESENTATION,
		ELEVATION,
		TERRAIN,
		FINISHED
	}
	
	/**
	 * implemented by classes that want to be informed about
	 * a conversion run's progress
	 */
	public static interface ProgressListener {
				
		/** announces the start of a new phase */
		public void updatePhase(Phase newPhase);
		
//		/** announces the fraction of the current phase that is completed */
//		public void updatePhaseProgress(float phaseProgress);

	}
	
	private List<ProgressListener> listeners = new ArrayList<ProgressListener>();
	
	public void addProgressListener(ProgressListener listener) {
		listeners.add(listener);
	}
	
	private void updatePhase(Phase newPhase) {
		for (ProgressListener listener : listeners) {
			listener.updatePhase(newPhase);
		}
	}
	
//	private void updatePhaseProgress(float phaseProgress) {
//		for (ProgressListener listener : listeners) {
//			listener.updatePhaseProgress(phaseProgress);
//		}
//	}
	
	/**
	 * exception to be thrown if the OSM input data covers an area
	 * larger than the maxBoundingBoxDegrees config property
	 */
	public static class BoundingBoxSizeException extends RuntimeException {
		
		public final Bound bound;

		private BoundingBoxSizeException(Bound bound) {
			this.bound = bound;
		}
		
		@Override
		public String toString() {
			return "oversized bounding box: " + bound;
		}
		
	}
	
}
