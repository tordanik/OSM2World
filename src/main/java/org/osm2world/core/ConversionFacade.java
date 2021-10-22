package org.osm2world.core;

import static java.lang.Math.abs;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Comparator.comparingDouble;
import static org.osm2world.core.math.AxisAlignedRectangleXZ.bbox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.time.StopWatch;
import org.osm2world.core.map_data.creation.LatLon;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.map_data.creation.MetricMapProjection;
import org.osm2world.core.map_data.creation.OSMToMapDataConverter;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.EleConstraintValidator;
import org.osm2world.core.map_elevation.creation.NoneEleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.SRTMData;
import org.osm2world.core.map_elevation.creation.TerrainElevationData;
import org.osm2world.core.map_elevation.creation.TerrainInterpolator;
import org.osm2world.core.map_elevation.creation.ZeroInterpolator;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.math.FaceXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.datastructures.IndexGrid;
import org.osm2world.core.math.datastructures.SpatialIndex;
import org.osm2world.core.osm.creation.OSMDataReader;
import org.osm2world.core.osm.creation.OSMFileReader;
import org.osm2world.core.osm.data.OSMData;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.util.FaultTolerantIterationUtil;
import org.osm2world.core.util.functions.Factory;
import org.osm2world.core.world.attachment.AttachmentConnector;
import org.osm2world.core.world.attachment.AttachmentSurface;
import org.osm2world.core.world.creation.WorldCreator;
import org.osm2world.core.world.creation.WorldModule;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.modules.AerowayModule;
import org.osm2world.core.world.modules.BarrierModule;
import org.osm2world.core.world.modules.BicycleParkingModule;
import org.osm2world.core.world.modules.BridgeModule;
import org.osm2world.core.world.modules.CliffModule;
import org.osm2world.core.world.modules.GolfModule;
import org.osm2world.core.world.modules.InvisibleModule;
import org.osm2world.core.world.modules.MastModule;
import org.osm2world.core.world.modules.ParkingModule;
import org.osm2world.core.world.modules.PoolModule;
import org.osm2world.core.world.modules.PowerModule;
import org.osm2world.core.world.modules.RailwayModule;
import org.osm2world.core.world.modules.RoadModule;
import org.osm2world.core.world.modules.SportsModule;
import org.osm2world.core.world.modules.StreetFurnitureModule;
import org.osm2world.core.world.modules.SurfaceAreaModule;
import org.osm2world.core.world.modules.TreeModule;
import org.osm2world.core.world.modules.TunnelModule;
import org.osm2world.core.world.modules.WaterModule;
import org.osm2world.core.world.modules.building.BuildingModule;
import org.osm2world.core.world.modules.building.indoor.IndoorModule;
import org.osm2world.core.world.modules.traffic_sign.TrafficSignModule;

import com.google.common.collect.Streams;

import de.topobyte.osm4j.core.resolve.EntityNotFoundException;

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
		private final MapData mapData;
		private final TerrainElevationData eleData;

		private Results(MapProjection mapProjection, MapData mapData, TerrainElevationData eleData) {
			this.mapProjection = mapProjection;
			this.mapData = mapData;
			this.eleData = eleData;
		}

		public MapProjection getMapProjection() {
			return mapProjection;
		}

		public MapData getMapData() {
			return mapData;
		}

		public TerrainElevationData getEleData() {
			return eleData;
		}

	}

	/**
	 * generates a default list of modules for the conversion
	 */
	static final List<WorldModule> createDefaultModuleList() {

		return Arrays.asList((WorldModule)
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
		);

	}

	private Function<LatLon, ? extends MapProjection> mapProjectionFactory = MetricMapProjection::new;

	private Factory<? extends TerrainInterpolator> terrainEleInterpolatorFactory = ZeroInterpolator::new;

	private Factory<? extends EleConstraintEnforcer> eleConstraintEnforcerFactory = NoneEleConstraintEnforcer::new;

	/**
	 * sets the factory that will make {@link MapProjection}
	 * instances during subsequent calls to
	 * {@link #createRepresentations(OSMData, List, Configuration, List)}.
	 */
	public void setMapProjectionFactory(Function<LatLon, ? extends MapProjection> mapProjectionFactory) {
		this.mapProjectionFactory = mapProjectionFactory;
	}

	/**
	 * sets the factory that will make {@link EleConstraintEnforcer}
	 * instances during subsequent calls to
	 * {@link #createRepresentations(OSMData, List, Configuration, List)}.
	 */
	public void setEleConstraintEnforcerFactory(
			Factory<? extends EleConstraintEnforcer> interpolatorFactory) {
		this.eleConstraintEnforcerFactory = interpolatorFactory;
	}

	/**
	 * sets the factory that will make {@link TerrainInterpolator}
	 * instances during subsequent calls to
	 * {@link #createRepresentations(OSMData, List, Configuration, List)}.
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
			List<? extends WorldModule> worldModules, Configuration config,
			List<? extends Target> targets)
			throws IOException {

		if (osmFile == null) {
			throw new IllegalArgumentException("osmFile must not be null");
		}

		OSMData osmData = new OSMFileReader(osmFile).getData();

		return createRepresentations(osmData, worldModules, config, targets);

	}

	/**
	 * variant of
	 * {@link #createRepresentations(File, List, Configuration, List)}
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
	 * @param targets       receivers of the conversion results; can be null if
	 *                      you want to handle the returned results yourself
	 *
	 * @throws BoundingBoxSizeException  for oversized bounding boxes
	 */
	public Results createRepresentations(OSMData osmData,
			List<? extends WorldModule> worldModules, Configuration config,
			List<? extends Target> targets)
			throws IOException, BoundingBoxSizeException {

		/* check the inputs */

		if (osmData == null) {
			throw new IllegalArgumentException("osmData must not be null");
		}

		if (config == null) {
			config = new BaseConfiguration();
		}

		Double maxBoundingBoxDegrees = config.getDouble("maxBoundingBoxDegrees", null);
		if (maxBoundingBoxDegrees != null
				&& (osmData.getLatLonBounds().sizeLat() > maxBoundingBoxDegrees
						|| osmData.getLatLonBounds().sizeLon() > maxBoundingBoxDegrees)) {
			throw new BoundingBoxSizeException();
		}

		/* create map data from OSM data */
		updatePhase(Phase.MAP_DATA);

		MapProjection mapProjection = mapProjectionFactory.apply(osmData.getCenter());

		OSMToMapDataConverter converter = new OSMToMapDataConverter(mapProjection, config);
		MapData mapData = null;
		try {
			mapData = converter.createMapData(osmData);
		} catch (EntityNotFoundException e) {
			// TODO: what to do here?
		}

		/* perform the rest of the conversion */

		return createRepresentations(mapProjection, mapData, worldModules, config, targets);

	}

	/**
	 * variant of {@link #createRepresentations(OSMData, List, Configuration, List)}
	 * that takes {@link MapData} instead of {@link OSMData}
	 */
	public Results createRepresentations(MapProjection mapProjection, MapData mapData,
			List<? extends WorldModule> worldModules, Configuration config,
			List<? extends Target> targets)
			throws IOException {

		/* check the inputs */

		if (mapData == null) {
			throw new IllegalArgumentException("osmData must not be null");
		}

		if (config == null) {
			config = new BaseConfiguration();
		}

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

		String srtmDir = config.getString("srtmDir", null);
		TerrainElevationData eleData = null;

		if (srtmDir != null) {
			eleData = new SRTMData(new File(srtmDir), mapProjection);
		}

		/* create terrain and attach connectors */
		updatePhase(Phase.TERRAIN);

		calculateElevations(mapData, eleData, config);
		attachConnectors(mapData);

		/* supply results to targets and caller */
		updatePhase(Phase.FINISHED);

		boolean underground = config.getBoolean("renderUnderground", true);

		if (targets != null) {
			for (Target target : targets) {
				TargetUtil.renderWorldObjects(target, mapData, underground);
				target.finish();
			}
		}

		return new Results(mapProjection, mapData, eleData);

	}

	private void attachConnectors(MapData mapData) {

		/* collect the surfaces */

		SpatialIndex<AttachmentSurface> attachmentSurfaceIndex =
				new IndexGrid<>(mapData.getDataBoundary().pad(50), 100, 100);

		for (WorldObject object : mapData.getWorldObjects()) {
			if (object.getParent() != null) continue;
			object.getAttachmentSurfaces().forEach(attachmentSurfaceIndex::insert);
		}

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

			Predicate<FaceXYZ> matchesPreferredHeight = (FaceXYZ f) -> {
				if (!requirePreferredHeight) {
					return true;
				} else {
					VectorXYZ closestPoint = f.closestPoint(posAtEle);
					double height = closestPoint.y - surface.getBaseEleAt(closestPoint.xz());
					return abs(height - connector.preferredHeight) < 0.001;
				}
			};

			Optional<FaceXYZ> closestFace = surface.getFaces().stream()
					.filter(matchesPreferredHeight)
					.filter(f -> connector.isAcceptableNormal.test(f.getNormal()))
					.min(comparingDouble(f -> connector.changeXZ ? f.distanceTo(posAtEle) : f.distanceToXZ(posAtEle)));

			if (!closestFace.isPresent()) continue; // try again without enforcing the preferred height

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
	 * uses OSM data and an terrain elevation data (usually from an external
	 * source) to calculate elevations for all {@link EleConnector}s of the
	 * {@link WorldObject}s
	 */
	private void calculateElevations(MapData mapData,
			TerrainElevationData eleData, Configuration config) {

		final TerrainInterpolator interpolator =
				(eleData != null)
				? terrainEleInterpolatorFactory.get()
				: new ZeroInterpolator();

		/* provide known elevations from eleData to the interpolator */

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		if (!(interpolator instanceof ZeroInterpolator)) {

			Collection<VectorXYZ> sites = emptyList();

			try {

				sites = eleData.getSites(mapData);

				System.out.println("time getSites: " + stopWatch);
				stopWatch.reset();
				stopWatch.start();

			} catch (IOException e) {
				e.printStackTrace();
			}

			interpolator.setKnownSites(sites);

			System.out.println("time setKnownSites: " + stopWatch);
			stopWatch.reset();
			stopWatch.start();

		}

		/* interpolate connectors' elevations */

		final List<EleConnector> connectors = new ArrayList<EleConnector>();

		FaultTolerantIterationUtil.forEach(mapData.getWorldObjects(), (WorldObject worldObject) -> {

			for (EleConnector conn : worldObject.getEleConnectors()) {
				conn.setPosXYZ(interpolator.interpolateEle(conn.pos));
				connectors.add(conn);
			}

		});

		System.out.println("time terrain interpolation: " + stopWatch);
		stopWatch.reset();
		stopWatch.start();

		/* enforce constraints defined by WorldObjects */

		boolean debugConstraints = config.getBoolean("debugConstraints", false);

		final EleConstraintEnforcer enforcer = debugConstraints
				? new EleConstraintValidator(mapData,
						eleConstraintEnforcerFactory.get())
				: eleConstraintEnforcerFactory.get();

		enforcer.addConnectors(connectors);

		if (!(enforcer instanceof NoneEleConstraintEnforcer)) {

			FaultTolerantIterationUtil.forEach(mapData.getWorldObjects(),
					(WorldObject o) -> o.defineEleConstraints(enforcer));

		}

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

		private static final long serialVersionUID = 2841146365929523046L; //generated VersionID

		private BoundingBoxSizeException() {}

		@Override
		public String toString() {
			return "oversized bounding box";
		}

	}

}
