package org.osm2world;

import static java.lang.Math.abs;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparingDouble;
import static java.util.Objects.requireNonNullElse;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bbox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.osm2world.conversion.ConfigUtil;
import org.osm2world.conversion.ConversionLog;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.creation.OSMToMapDataConverter;
import org.osm2world.map_data.data.MapData;
import org.osm2world.map_elevation.creation.*;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.datastructures.IndexGrid;
import org.osm2world.math.datastructures.SpatialIndex;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.shapes.FlatSimplePolygonShapeXYZ;
import org.osm2world.osm.creation.OSMDataReader;
import org.osm2world.osm.creation.OSMFileReader;
import org.osm2world.osm.data.OSMData;
import org.osm2world.output.Output;
import org.osm2world.output.OutputUtil;
import org.osm2world.output.common.material.Materials;
import org.osm2world.output.common.model.Models;
import org.osm2world.util.FaultTolerantIterationUtil;
import org.osm2world.util.functions.Factory;
import org.osm2world.world.attachment.AttachmentConnector;
import org.osm2world.world.attachment.AttachmentSurface;
import org.osm2world.world.creation.WorldCreator;
import org.osm2world.world.creation.WorldModule;
import org.osm2world.world.data.WorldObject;
import org.osm2world.world.modules.*;
import org.osm2world.world.modules.building.BuildingModule;
import org.osm2world.world.modules.building.indoor.IndoorModule;
import org.osm2world.world.modules.traffic_sign.TrafficSignModule;

import com.google.common.collect.Streams;

import de.topobyte.osm4j.core.resolve.EntityNotFoundException;

/**
 * provides an easy way to call all steps of the conversion process in the correct order.
 * External users of OSM2World should prefer {@link O2WConverter}, which will eventually replace this class.
 */
public class ConversionFacade {

	/**
	 * all results of a conversion run
	 */
	public static final class Results {

		private final @Nullable MapProjection mapProjection;
		private final MapData mapData;

		private Results(@Nullable MapProjection mapProjection, MapData mapData) {
			this.mapProjection = mapProjection;
			this.mapData = mapData;
		}

		public MapProjection getMapProjection() {
			return mapProjection;
		}

		public MapData getMapData() {
			return mapData;
		}

	}

	/**
	 * generates a default list of modules for the conversion
	 */
	static final List<WorldModule> createDefaultModuleList(O2WConfig config) {

		List<String> excludedModules = config.getList("excludeWorldModule")
			.stream().map(m -> m.toString()).toList();

		return Stream.of((WorldModule)
			new RoadModule(),
			new RailwayModule(),
			new AerowayModule(),
			new BuildingModule(),
			new ParkingModule(),
			new TreeModule(),
			new StreetFurnitureModule(),
			new TrafficSignModule(),
			new BicycleParkingModule(),
			new WaterModule(),
			new PoolModule(),
			new GolfModule(),
			new SportsModule(),
			new CliffModule(),
			new BarrierModule(),
			new PowerModule(),
			new MastModule(),
			new BridgeModule(),
			new TunnelModule(),
			new SurfaceAreaModule(),
			new InvisibleModule(),
			new IndoorModule()
		)
		.filter(m -> !excludedModules.contains(m.getClass().getSimpleName()))
		.toList();
	}

	private @Nullable Function<LatLon, ? extends MapProjection> mapProjectionFactory = null;

	private @Nullable Factory<? extends TerrainInterpolator> terrainEleInterpolatorFactory = null;
	private @Nullable Factory<? extends EleCalculator> eleCalculatorFactory = null;

	/**
	 * sets the factory that will make {@link MapProjection} instances during subsequent uses.
	 * Can be set to null, in which case there will be an attempt to parse the configuration for this.
	 */
	public void setMapProjectionFactory(@Nullable Function<LatLon, ? extends MapProjection> mapProjectionFactory) {
		this.mapProjectionFactory = mapProjectionFactory;
	}

	/**
	 * sets the factory that will make {@link EleCalculator} instances during subsequent uses.
	 * Can be set to null, in which case there will be an attempt to parse the configuration for this.
	 */
	public void setEleCalculatorFactory(@Nullable Factory<? extends EleCalculator> eleCalculatorFactory) {
		this.eleCalculatorFactory = eleCalculatorFactory;
	}

	/**
	 * sets the factory that will make {@link TerrainInterpolator} instances during subsequent uses.
	 *  Can be set to null, in which case there will be an attempt to parse the configuration for this.
	 */
	public void setTerrainEleInterpolatorFactory(@Nullable Factory<? extends TerrainInterpolator> enforcerFactory) {
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
	 * @param outputs       receivers of the conversion results; can be null if
	 *                      you want to handle the returned results yourself
	 */
	public Results createRepresentations(File osmFile,
			@Nullable List<? extends WorldModule> worldModules, @Nullable O2WConfig config,
			@Nullable List<? extends Output> outputs)
			throws IOException {

		if (osmFile == null) {
			throw new IllegalArgumentException("osmFile must not be null");
		}

		OSMData osmData = new OSMFileReader(osmFile).getAllData();

		return createRepresentations(osmData, worldModules, config, outputs);

	}

	/**
	 * variant of {@link #createRepresentations(File, List, O2WConfig, List)}
	 * that accepts {@link OSMData} instead of a file.
	 * Use this when all data is already
	 * in memory, for example with editor applications.
	 * To obtain the data, you can use an {@link OSMDataReader}.
	 *
	 * @param osmData       input data; != null
	 * @param worldModules  modules that will create the {@link WorldObject}s
	 *                      in the result; null to use a default module list
	 * @param config        set of parameters that controls various aspects
	 *                      of the modules' behavior; null to use defaults
	 * @param outputs       receivers of the conversion results; can be null if
	 *                      you want to handle the returned results yourself
	 */
	public Results createRepresentations(OSMData osmData,
			@Nullable List<? extends WorldModule> worldModules, @Nullable O2WConfig config,
			@Nullable List<? extends Output> outputs)
			throws IOException {

		/* check the inputs */

		if (osmData == null) {
			throw new IllegalArgumentException("osmData must not be null");
		}

		if (config == null) {
			config = new O2WConfig();
		}

		/* create map data from OSM data */
		updatePhase(Phase.MAP_DATA);

		var mpFactory = requireNonNullElse(this.mapProjectionFactory, config.mapProjection());
		MapProjection mapProjection = mpFactory.apply(osmData.getCenter());

		OSMToMapDataConverter converter = new OSMToMapDataConverter(mapProjection);
		MapData mapData;
		try {
			mapData = converter.createMapData(osmData, config);
		} catch (EntityNotFoundException e) {
			throw new IOException(e);
		}

		/* perform the rest of the conversion */

		return createRepresentations(mapProjection, mapData, worldModules, config, outputs);

	}

	/**
	 * variant of {@link #createRepresentations(OSMData, List, O2WConfig, List)}
	 * that takes {@link MapData} instead of {@link OSMData}
	 *
	 * @param mapProjection  projection for converting between {@link LatLon} and local coordinates in {@link MapData}.
	 *                       May be null, but that prevents accessing additional data sources such as {@link SRTMData}.
	 */
	public Results createRepresentations(@Nullable MapProjection mapProjection, MapData mapData,
			@Nullable List<? extends WorldModule> worldModules, @Nullable O2WConfig config,
			@Nullable List<? extends Output> outputs) {

		/* check the inputs */

		if (mapData == null) {
			throw new IllegalArgumentException("map data is required");
		}

		if (config == null) {
			config = new O2WConfig();
		}

		/* apply world modules */
		updatePhase(Phase.REPRESENTATION);

		if (worldModules == null) {
			worldModules = createDefaultModuleList(config);
		}

		ConfigUtil.parseFonts(config);
		Materials.configureMaterials(config);
		Models.configureModels(config);
			//this will cause problems if multiple conversions are run
			//at the same time, because global variables are being modified

		WorldCreator moduleManager = new WorldCreator(config, worldModules);
		moduleManager.addRepresentationsTo(mapData);

		/* determine elevations */
		updatePhase(Phase.ELEVATION);

		TerrainElevationData eleData = null;

		if (config.srtmDir() != null) {
			if (mapProjection == null) {
				throw new IllegalArgumentException("Using SRTM data requires a map projection");
			}
			eleData = new SRTMData(config.srtmDir(), mapProjection);
		}

		/* create terrain and attach connectors */
		updatePhase(Phase.TERRAIN);

		calculateElevations(mapData, eleData, config);
		attachConnectors(mapData);

		/* convert 3d scene to target representation */
		updatePhase(Phase.OUTPUT);

		boolean underground = config.getBoolean("renderUnderground", true);

		if (outputs != null) {
			for (Output output : outputs) {
				output.setConfiguration(config);
				OutputUtil.renderWorldObjects(output, mapData, underground);
				output.finish();
			}
		}

		/* supply results to outputs */
		updatePhase(Phase.FINISHED);

		return new Results(mapProjection, mapData);

	}

	private void attachConnectors(MapData mapData) {

		/* collect the surfaces */

		SpatialIndex<AttachmentSurface> attachmentSurfaceIndex =
				new IndexGrid<>(mapData.getDataBoundary().pad(50), 100, 100);

		FaultTolerantIterationUtil.forEach(mapData.getWorldObjects(), object -> {
			if (object.getParent() == null) {
				object.getAttachmentSurfaces().forEach(attachmentSurfaceIndex::insert);
			}
		});

		/* attach connectors to the surfaces */

		for (WorldObject object : mapData.getWorldObjects()) {

			if (object.getParent() != null) continue;

			for (AttachmentConnector connector : object.getAttachmentConnectors()) {

				for (String surfaceType : connector.compatibleSurfaceTypes) {

					Iterable<AttachmentSurface> nearbySurfaces = attachmentSurfaceIndex.probe(
							bbox(singleton(connector.originalPos)).pad(connector.maxDistanceXZ()));

					Optional<AttachmentSurface> closestSurface = Streams.stream(nearbySurfaces)
							.filter(s -> s.getTypes().stream().anyMatch(t -> t.equals(surfaceType)))
							.filter(s -> s.getFaces().stream().anyMatch(f -> connector.isAcceptableNormal.test(f.getNormal())))
							.min(comparingDouble(s -> s.distanceTo(connector.originalPos)));

					if (closestSurface.isPresent()) {
						attachConnectorIfValid(connector, closestSurface.get());
						break;
					}
				}

			}

		}

	}

	protected static void attachConnectorIfValid(AttachmentConnector connector, AttachmentSurface surface) {

		double ele = surface.getBaseEleAt(connector.originalPos.xz()) + connector.preferredHeight;
		VectorXYZ posAtEle = connector.originalPos.y(ele);

		for (boolean requirePreferredHeight : asList(true, false)) {

			Predicate<FlatSimplePolygonShapeXYZ> matchesPreferredHeight = f -> {
				if (!requirePreferredHeight) {
					return true;
				} else {
					VectorXYZ closestPoint = f.closestPoint(posAtEle);
					double height = closestPoint.y - surface.getBaseEleAt(closestPoint.xz());
					return abs(height - connector.preferredHeight) < 0.001;
				}
			};

			Optional<? extends FlatSimplePolygonShapeXYZ> closestFace = surface.getFaces().stream()
					.filter(matchesPreferredHeight)
					.filter(f -> connector.isAcceptableNormal.test(f.getNormal()))
					.min(comparingDouble(f -> connector.changeXZ ? f.distanceTo(posAtEle) : f.distanceToXZ(posAtEle)));

			if (closestFace.isEmpty()) continue; // try again without enforcing the preferred height

			VectorXYZ closestPoint = null;

			if (!connector.changeXZ && closestFace.get().getNormal().y >= 0.001) {
				// no XZ movement is desired, obtain the face point directly above/below the connector
				VectorXYZ pointInFacePlane = posAtEle.y(closestFace.get().getYAt(posAtEle.xz()));
				closestPoint = closestFace.get().closestPoint(pointInFacePlane);
			} else {
				closestPoint = closestFace.get().closestPoint(posAtEle);
			}

			if (closestPoint.xz().distanceTo(connector.originalPos.xz()) > connector.maxDistanceXZ() + 0.001) {
				continue;
			}

			connector.attach(surface, closestPoint, closestFace.get().getNormal());
			break; // attached, don't try again

		}

	}

	/**
	 * uses OSM data and a terrain elevation data (usually from an external
	 * source) to calculate elevations for all {@link EleConnector}s of the
	 * {@link WorldObject}s
	 */
	private void calculateElevations(MapData mapData,
			TerrainElevationData eleData, O2WConfig config) {

		TerrainInterpolator interpolator = requireNonNullElse(terrainEleInterpolatorFactory, config.terrainInterpolator()).get();

		if (eleData == null) {
			interpolator = new ZeroInterpolator();
		}

		/* provide known elevations from eleData to the interpolator */

		if (!(interpolator instanceof ZeroInterpolator)) {

			Collection<VectorXYZ> sites = emptyList();

			try {
				sites = eleData.getSites(mapData.getDataBoundary().pad(10));
			} catch (IOException e) {
				ConversionLog.error("Could not read elevation data: " + e.getMessage(), e);
			}

			if (!sites.isEmpty()) {
				interpolator.setKnownSites(sites);
			} else {
				ConversionLog.error("No sites with known elevation available");
				interpolator = new ZeroInterpolator();
			}

		}

		/* interpolate terrain elevation for each connector */

		final TerrainInterpolator finalInterpolator = interpolator;

		FaultTolerantIterationUtil.forEach(mapData.getWorldObjects(), (WorldObject worldObject) -> {
			for (EleConnector conn : worldObject.getEleConnectors()) {
				conn.setPosXYZ(finalInterpolator.interpolateEle(conn.pos));
			}
		});

		/* refine terrain-based elevation with information from map data */

		EleCalculator eleCalculator = requireNonNullElse(eleCalculatorFactory, config.eleCalculator()).get();
		eleCalculator.calculateElevations(mapData);

	}

	public enum Phase {
		MAP_DATA,
		REPRESENTATION,
		ELEVATION,
		TERRAIN,
		OUTPUT,
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
