package org.osm2world.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.osm2world.core.heightmap.creation.FlatTerrainElevation;
import org.osm2world.core.heightmap.data.CellularTerrainElevation;
import org.osm2world.core.map_data.creation.HackMapProjection;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.map_data.creation.OSMToMapDataConverter;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_elevation.creation.ElevationCalculator;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.osm.creation.JOSMFileHack;
import org.osm2world.core.osm.creation.OsmosisReader;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.terrain.creation.TerrainCreator;
import org.osm2world.core.terrain.data.Terrain;
import org.osm2world.core.world.creation.WorldCreator;
import org.osm2world.core.world.creation.WorldModule;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.modules.BarrierModule;
import org.osm2world.core.world.modules.BridgeModule;
import org.osm2world.core.world.modules.BuildingModule;
import org.osm2world.core.world.modules.ParkingModule;
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

	//TODO: allow configuration options in or after constructor
	
	/**
	 * all results of a conversion run
	 */
	public static final class Results {
		
		private final MapProjection mapProjection;
		private final MapData map;
		private final Terrain terrain;
		private final CellularTerrainElevation eleData;
		
		public Results(MapProjection mapProjection, MapData grid,
				Terrain terrain, CellularTerrainElevation eleData) {
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
		
		public Terrain getTerrain() {
			return terrain;
		}
		
		/*
		 * TODO: remove in the future, because it isn't really a conversion result.
		 * Once real elevation data is available, this should be a conversion *input*
		 */
		public CellularTerrainElevation getEleData() {
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
				new BarrierModule(),
				new BridgeModule(),
				new TunnelModule(),
				new SurfaceAreaModule()
		);
		
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
	 */
	public Results createRepresentations(File osmFile,
			List<WorldModule> worldModules, Configuration config)
			throws IOException {
		
		if (osmFile == null) {
			throw new IllegalArgumentException("osmFile must not be null");
		}
		
		OSMData osmData;
		
		try {
			osmData = new OsmosisReader(osmFile).getData();
		} catch (IOException e) {
			
			System.out.println("could not read file," +
					" trying workaround for files created by JOSM");
			
			File tempFile;
			try {
				tempFile = JOSMFileHack.createTempOSMFile(osmFile);
			} catch (Exception e2) {
				throw new IOException("could not create temporary" +
						" modified copy of the file");
			}
			osmData = new OsmosisReader(tempFile).getData();
			
		}
		
		return createRepresentations(osmData, worldModules, config);
		
	}
	
	
	/**
	 * variant of {@link #createRepresentations(File)} that accepts
	 * {@link OSMData} instead of a file. Use this when all data is already
	 * in memory, for example with editor applications.
	 * 
	 * @param osmData       input data; != null
	 * @param worldModules  modules that will create the {@link WorldObject}s
	 *                      in the result; null to use a default module list
	 * @param config        set of parameters that controls various aspects
	 *                      of the modules' behavior; null to use defaults
	 */
	public Results createRepresentations(OSMData osmData,
			List<WorldModule> worldModules, Configuration config)
			throws IOException {
		
		if (osmData == null) {
			throw new IllegalArgumentException("osmData must not be null");
		}
		
		/* create grid from OSM data */
		updatePhase(Phase.MAP_DATA);
		
		MapProjection mapProjection = new HackMapProjection(osmData);
		OSMToMapDataConverter converter = new OSMToMapDataConverter(mapProjection);
		MapData grid = converter.createMapData(osmData);
		
		/* apply world modules */
		updatePhase(Phase.REPRESENTATION);
		
		if (config == null) {
			config = new BaseConfiguration();
		}
		if (worldModules == null) {
			worldModules = createDefaultModuleList();
		}
		
		WorldCreator moduleManager =
			new WorldCreator(config, worldModules);
		moduleManager.addRepresentationsTo(grid);
		
		/* determine elevations */
		updatePhase(Phase.ELEVATION);
		
		CellularTerrainElevation eleData = createEleData(grid);
		
		new ElevationCalculator().calculateElevations(grid, eleData);
		
		/* create terrain */
		updatePhase(Phase.TERRAIN);
		
		Terrain terrain = new TerrainCreator().createTerrain(grid, eleData);
		
		/* return results */
		updatePhase(Phase.FINISHED);
		
		return new Results(mapProjection, grid, terrain, eleData);
		
	}
	
	/**
	 * generates some fake elevation data;
	 * will no longer be necessary when real data can be used
	 */
	private static CellularTerrainElevation createEleData(MapData grid) {
		
		AxisAlignedBoundingBoxXZ terrainBoundary = grid.getBoundary().pad(10.0);
		
		int numPointsX = Math.max(2, (int) (terrainBoundary.sizeX() / 30));
		int numPointsZ = Math.max(2, (int) (terrainBoundary.sizeZ() / 30));
				
		CellularTerrainElevation eleData = new FlatTerrainElevation(
				terrainBoundary,
				numPointsX, numPointsZ); //TODO: change to distance between points
		
		return eleData;
		
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
	
}
