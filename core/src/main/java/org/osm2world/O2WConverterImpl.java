package org.osm2world;

import static java.lang.Math.ceil;
import static java.time.Instant.now;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparingDouble;
import static java.util.Objects.requireNonNullElse;
import static org.osm2world.conversion.ConversionLog.LogLevel.FATAL;
import static org.osm2world.conversion.ProgressListener.Phase.FINISHED;
import static org.osm2world.conversion.ProgressListener.Phase.values;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bbox;
import static org.osm2world.world.connectors.Connector.AttachmentProperties;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.osm2world.conversion.ConversionLog;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.conversion.ProgressListener;
import org.osm2world.map_data.creation.OSMToMapDataConverter;
import org.osm2world.map_data.data.MapData;
import org.osm2world.map_elevation.creation.*;
import org.osm2world.math.datastructures.IndexGrid;
import org.osm2world.math.datastructures.SpatialIndex;
import org.osm2world.math.geo.GeoBounds;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.geo.TileNumber;
import org.osm2world.osm.creation.OSMDataReader;
import org.osm2world.osm.creation.OSMDataReaderView;
import org.osm2world.osm.data.OSMData;
import org.osm2world.output.Output;
import org.osm2world.output.common.compression.Compression;
import org.osm2world.output.common.compression.CompressionUtil;
import org.osm2world.scene.Scene;
import org.osm2world.util.FaultTolerantIterationUtil;
import org.osm2world.util.platform.json.JsonUtil;
import org.osm2world.world.connectors.AttachmentSurface;
import org.osm2world.world.connectors.Connector;
import org.osm2world.world.creation.WorldCreator;
import org.osm2world.world.creation.WorldModule;
import org.osm2world.world.data.WorldObject;
import org.osm2world.world.modules.*;
import org.osm2world.world.modules.building.BuildingModule;
import org.osm2world.world.modules.building.indoor.IndoorModule;
import org.osm2world.world.modules.traffic_sign.TrafficSignModule;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;

import de.topobyte.osm4j.core.resolve.EntityNotFoundException;

/**
 * Implementation of the <code>O2WConverter</code> class available in core-jvm for library users.
 * See that class for documentation.
 * Provides an easy way to call all steps of the conversion process in the correct order.
 */
class O2WConverterImpl {

	private final O2WConfig config;
	private final List<ProgressListener> listeners;

	O2WConverterImpl(@Nullable O2WConfig config, List<ProgressListener> listeners) {
		this.config = requireNonNullElse(config, new O2WConfig());
		this.listeners = new ArrayList<>(listeners);
	}

	Scene convert(OSMDataReader osmDataReader, @Nullable GeoBounds bounds, @Nullable MapProjection mapProjection,
			Output... outputs) throws IOException{

		if (osmDataReader == null) {
			throw new IllegalArgumentException("osmDataReader is required");
		}

		setUpLogging();
		var perfListener = new PerformanceListener();

		try {

			/* load OSM data */

			updatePhase(perfListener, ProgressListener.Phase.MAP_DATA);

			OSMData osmData;

			if (bounds instanceof TileNumber tile) {
				osmData = osmDataReader.getData(tile);
			} else if (bounds != null) {
				osmData = osmDataReader.getData(bounds.latLonBounds());
			} else {
				osmData = osmDataReader.getAllData();
			}

			/* create map data from OSM data */

			mapProjection = requireNonNullElse(mapProjection, config.mapProjection().apply(osmData.getCenter()));

			OSMToMapDataConverter converter = new OSMToMapDataConverter(mapProjection);

			MapData mapData = converter.createMapData(osmData, config);

			return this.runConversion(mapData, mapProjection, outputs, perfListener);

		} catch (EntityNotFoundException e) {
			@Nullable String name = buildConversionLogName(bounds, osmDataReader);
			String message = "Conversion failed" + ((name != null) ? " (" + name + ")" : "");
			ConversionLog.log(FATAL, message, e, null);
			throw new IOException(e);
		} catch (Exception e) {
			@Nullable String name = buildConversionLogName(bounds, osmDataReader);
			String message = "Conversion failed" + ((name != null) ? " (" + name + ")" : "");
			ConversionLog.log(FATAL, message, e, null);
			throw e;
		} finally {
			writeLogs(buildConversionLogName(bounds, osmDataReader), perfListener, config);
		}

	}

	Scene convert(MapData mapData, @Nullable MapProjection mapProjection, Output... outputs) {

		if (mapData == null) {
			throw new IllegalArgumentException("mapData is required");
		}

		setUpLogging();
		var perfListener = new PerformanceListener();

		try {

			return runConversion(mapData, mapProjection, outputs, perfListener);

		} catch (Exception e) {
			ConversionLog.log(FATAL, "Conversion failed", e, null);
			throw e;
		} finally {
			writeLogs(null, perfListener, config);
		}

	}

	private Scene runConversion(MapData mapData, MapProjection mapProjection, Output[] outputs,
			PerformanceListener perfListener) {

		outputs = requireNonNullElse(outputs, new Output[0]);

		/* apply world modules */
		updatePhase(perfListener, ProgressListener.Phase.REPRESENTATION);

		WorldCreator moduleManager = new WorldCreator(config, createModuleList(config));
		moduleManager.addRepresentationsTo(mapData);

		/* determine elevations */
		updatePhase(perfListener, ProgressListener.Phase.ELEVATION);

		TerrainEleDataSource eleData = TerrainEleDataUtil.eleDataSourceFromConfig(config);

		if (eleData != null && mapProjection == null) {
			throw new IllegalArgumentException("Using elevation data requires a map projection");
		}

		/* create terrain and attach connectors */
		updatePhase(perfListener, ProgressListener.Phase.TERRAIN);

		calculateElevations(mapData, mapProjection, eleData, config);
		attachConnectors(mapData);

		/* convert 3d scene to target representation */
		updatePhase(perfListener, ProgressListener.Phase.OUTPUT);

		var scene = new Scene(mapProjection, mapData);

		/* supply results to outputs */

		for (Output output : outputs) {
			output.setConfiguration(config);
			output.outputScene(scene);
		}

		updatePhase(perfListener, ProgressListener.Phase.FINISHED);

		return scene;

	}

	/**
	 * generates the list of {@link WorldModule}s for the conversion
	 */
	private static List<WorldModule> createModuleList(O2WConfig config) {

		List<String> excludedModules = config.getList("excludeWorldModule");

		return Stream.of((WorldModule)
						new ExternalModelModule(),
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

	/**
	 * attaches connectors to {@link AttachmentSurface}s.
	 * <p>
	 * This happens in order of dependencies: A connector waits until all objects with nearby compatible surfaces
	 * have attached their own connectors and updated their surfaces accordingly.
	 * This allows an object to attach to another object which is itself attached to something,
	 * e.g. a traffic sign attached to a street lamp on a roof.
	 * If objects depend on each other in a cycle, one of the connectors is attached without waiting.
	 */
	private void attachConnectors(MapData mapData) {

		/* collect the surfaces */

		IndexGrid<AttachmentSurface> surfaceIndex = new IndexGrid<>(mapData.getDataBoundary().pad(50), 100, 100);

		Map<WorldObject, List<AttachmentSurface>> surfacesByObject = new IdentityHashMap<>();

		FaultTolerantIterationUtil.forEach(mapData.getWorldObjects(), object -> {
			if (object.getParent() == null) {
				List<AttachmentSurface> surfaces = List.copyOf(object.getAttachmentSurfaces());
				surfacesByObject.put(object, surfaces);
				surfaces.forEach(surfaceIndex::insert);
			}
		});

		/* collect the connectors which should be attached, grouped by object */

		Map<WorldObject, List<Connector>> pendingConnectors = new LinkedHashMap<>();

		FaultTolerantIterationUtil.forEach(mapData.getWorldObjects(), object -> {
			if (object.getParent() == null) {
				List<Connector> connectors = new ArrayList<>();
				for (Connector connector : object.getConnectors()) {
					if (connector.attachmentProperties != null) {
						connectors.add(connector);
					}
				}
				if (!connectors.isEmpty()) {
					pendingConnectors.put(object, connectors);
				}
			}
		});

		/* attach connectors, deferring those which depend on objects with pending connectors */

		boolean forceNext = false;

		while (!pendingConnectors.isEmpty()) {

			boolean progress = false;

			for (var it = pendingConnectors.entrySet().iterator(); it.hasNext(); ) {

				var entry = it.next();
				WorldObject object = entry.getKey();

				for (var connectorIt = entry.getValue().iterator(); connectorIt.hasNext(); ) {

					Connector connector = connectorIt.next();

					try {

						CandidateSurfaces candidates = findCandidateSurfaces(connector, surfaceIndex);

						boolean dependsOnPendingObject = candidates.surfaces().stream()
								.map(s -> getRootObject(s.getWorldObject()))
								.anyMatch(o -> o != null && o != object && pendingConnectors.containsKey(o));

						if (dependsOnPendingObject) {
							if (forceNext) {
								var element = object.getPrimaryMapElement();
								ConversionLog.warn("Cyclic attachment dependency, attaching connector without waiting",
										element != null ? element.getElementWithId() : null);
								forceNext = false;
							} else {
								continue;
							}
						}

						attachToCandidateSurface(connector, candidates);

					} catch (Exception e) {
						FaultTolerantIterationUtil.DEFAULT_EXCEPTION_HANDLER.accept(e, object);
					}

					connectorIt.remove();
					progress = true;

				}

				if (entry.getValue().isEmpty()) {
					it.remove();
					updateSurfaces(object, surfacesByObject.getOrDefault(object, List.of()), surfaceIndex);
				}

			}

			if (!progress) {
				forceNext = true;
			}

		}

	}

	/** returns the object at the top of the {@link WorldObject#getParent()} hierarchy */
	private static @Nullable WorldObject getRootObject(@Nullable WorldObject object) {
		while (object != null && object.getParent() != null) {
			object = object.getParent();
		}
		return object;
	}

	/** surfaces a connector might attach to, all of which have the given type */
	private record CandidateSurfaces(@Nullable String surfaceType, List<AttachmentSurface> surfaces) {}

	/**
	 * returns the surfaces which a connector might attach to.
	 * These are the nearby surfaces of the first compatible surface type (in order of preference) with any matches.
	 */
	private static CandidateSurfaces findCandidateSurfaces(Connector connector,
			SpatialIndex<AttachmentSurface> surfaceIndex) {

		AttachmentProperties ap = connector.attachmentProperties;
		if (ap == null) return new CandidateSurfaces(null, List.of());

		Iterable<AttachmentSurface> nearbySurfaces = surfaceIndex.probe(
				bbox(singleton(connector.originalPos)).pad(ap.maxDistanceXZ()));

		for (String surfaceType : ap.compatibleSurfaceTypes()) {

			List<AttachmentSurface> compatibleSurfaces = Streams.stream(nearbySurfaces)
					.filter(s -> s.getTypes().contains(surfaceType))
					.filter(s -> s.getFaces().stream().anyMatch(f -> ap.isAcceptableNormal().test(f.getNormal())))
					.toList();

			if (!compatibleSurfaces.isEmpty()) {
				return new CandidateSurfaces(surfaceType, compatibleSurfaces);
			}

		}

		return new CandidateSurfaces(null, List.of());

	}

	/**
	 * selects the most suitable of the candidate surfaces from {@link #findCandidateSurfaces(Connector, SpatialIndex)}
	 * and attaches the connector to it (if possible)
	 */
	private static void attachToCandidateSurface(Connector connector, CandidateSurfaces candidateSurfaces) {

		List<AttachmentSurface> candidates = candidateSurfaces.surfaces();

		Optional<AttachmentSurface> candidateSurface;

		if ("roof".equals(candidateSurfaces.surfaceType())) {
			// prioritize the topmost roof to avoid attaching to the hidden "roofs" of lower building parts
			double minDistanceXZ = candidates.stream()
					.mapToDouble(s -> s.distanceToXZ(connector.originalPos)).min().orElse(0);
			candidateSurface = candidates.stream()
					.filter(s -> s.distanceToXZ(connector.originalPos) < minDistanceXZ + 0.1)
					.max(comparingDouble(s -> s.closestPoint(connector.originalPos).y));
		} else {
			// choose the closest surface by 3D distance
			candidateSurface = candidates.stream()
					.min(comparingDouble(s -> s.distanceTo(connector.originalPos)));
		}

		candidateSurface.ifPresent(s -> attachConnectorIfValid(connector, s));

	}

	/**
	 * updates an object's surfaces after its connectors have been attached, and updates the spatial index
	 */
	private static void updateSurfaces(WorldObject object, List<AttachmentSurface> surfaces,
			IndexGrid<AttachmentSurface> surfaceIndex) {

		if (surfaces.isEmpty()) return;

		surfaces.forEach(surfaceIndex::remove);

		try {
			object.updateAttachmentSurfaces(surfaces);
		} catch (Exception e) {
			FaultTolerantIterationUtil.DEFAULT_EXCEPTION_HANDLER.accept(e, object);
		}

		surfaces.forEach(surfaceIndex::insert);

	}

	static void attachConnectorIfValid(Connector connector, AttachmentSurface surface) {

		AttachmentProperties ap = connector.attachmentProperties;
		if (ap == null) return;

		AttachmentSurface.AttachmentPoint point = surface.findAttachmentPoint(ap, connector.originalPos);

		if (point != null) {
			connector.attach(surface, point.pos(), point.normal());
		}

	}

	/**
	 * uses OSM data and terrain elevation data (usually from an external
	 * source) to calculate elevations for all {@link Connector}s of the
	 * {@link WorldObject}s
	 */
	private void calculateElevations(MapData mapData, MapProjection projection,
			TerrainEleDataSource eleData, O2WConfig config) {

		TerrainInterpolator interpolator = config.terrainInterpolator().get();

		if (eleData == null) {
			interpolator = new ZeroInterpolator();
		}

		/* provide known elevations from eleData to the interpolator */

		if (!(interpolator instanceof ZeroInterpolator)) {

			try {

				TerrainEleData sites = eleData.getSites(mapData.getDataBoundary().pad(10), projection);

				if (!sites.isEmpty()) {
					interpolator.setKnownSites(sites, projection);
				} else {
					ConversionLog.error("No sites with known elevation available");
					interpolator = new ZeroInterpolator();
				}

			} catch (IOException e) {
				ConversionLog.error("Could not read elevation data: " + e.getMessage(), e);
			}

		}

		/* interpolate terrain elevation for each connector */

		final TerrainInterpolator finalInterpolator = interpolator;

		FaultTolerantIterationUtil.forEach(mapData.getWorldObjects(), (WorldObject worldObject) -> {
			for (Connector conn : worldObject.getConnectors()) {
				conn.setPosXYZ(finalInterpolator.interpolateEle(conn.pos));
			}
		});

		/* refine terrain-based elevation with information from map data */

		EleCalculator eleCalculator = config.eleCalculator().get();
		eleCalculator.calculateElevations(mapData);

	}

	private void updatePhase(PerformanceListener perfListener, ProgressListener.Phase newPhase) {
		double progress = newPhase.ordinal() * 1.0 / (ProgressListener.Phase.values().length - 1);
		for (ProgressListener listener : Iterables.concat(listeners, List.of(perfListener))) {
			listener.updateProgress(newPhase, progress);
		}
	}

	private void setUpLogging() {

		// clear ConversionLog (might have entries from previous convert runs on the same thread)
		ConversionLog.clear();

		ConversionLog.setConsoleLogLevels(config.consoleLogLevels());

	}

	/** tries to return a name for the log (usually a tile number) */
	private static @Nullable String buildConversionLogName(GeoBounds bounds, OSMDataReader osmDataReader) throws IOException {
		GeoBounds b = (bounds != null) ? bounds
				: (osmDataReader instanceof OSMDataReaderView view) ? view.getBounds() : null;
		return b instanceof TileNumber tile ? tile.toString("_") : null;
	}

	private static void writeLogs(@Nullable String fileNameSuffix, PerformanceListener perfListener, O2WConfig config) {

		@Nullable File logDir = config.logDir();

		if (logDir == null) return;

		logDir.mkdirs();

		fileNameSuffix = Objects.requireNonNullElse(fileNameSuffix,
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm_ss")));
		String fileNameBase = "osm2world_log_" + fileNameSuffix;

		double totalTime = Duration.between(perfListener.startTime, now()).toMillis() / 1000.0;

		Map<ProgressListener.Phase, Double> timePerPhase;
		if (perfListener.currentPhase == FINISHED) {
			var phaseDurations = perfListener.getPhaseDurations();
			timePerPhase = Maps.transformValues(phaseDurations, it -> it.toMillis() / 1000.0);
		} else {
			timePerPhase = Map.of();
		}

		/* write a json file with performance stats */

		try (FileWriter writer = new FileWriter(new File(logDir, fileNameBase + ".json"))) {

			Map<String, Object> jsonRoot = Map.of(
					"startTime", perfListener.startTime.toString(),
					"totalTime", totalTime,
					"timePerPhase", timePerPhase
			);

			JsonUtil.toJson(jsonRoot, writer, true);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		/* write a gz-compressed text file with the error log */

		Compression compression = Compression.GZ;
		File outputFile = new File(logDir, fileNameBase + ".txt.gz");
		CompressionUtil.writeFileWithCompression(outputFile, compression, outputStream -> {
			try (var printStream = new PrintStream(outputStream)) {

				printStream.println("Runtime (seconds):\nTotal: " + totalTime);

				if (!timePerPhase.isEmpty()) {
					for (ProgressListener.Phase phase : values()) {
						if (timePerPhase.containsKey(phase)) {
							printStream.println(phase + ": " + timePerPhase.get(phase));
						}
					}
				}

				printStream.println();

				List<ConversionLog.Entry> entries = ConversionLog.getLog();
				int maxLogEntries = config.maxLogEntries();

				if (entries.size() <= maxLogEntries) {
					entries.forEach(printStream::println);
				} else {
					IntStream.range(0, maxLogEntries / 2)
							.mapToObj(entries::get).forEach(printStream::println);
					printStream.println("\n...\n");
					IntStream.range(entries.size() - (int)ceil(maxLogEntries / 2.0), entries.size())
							.mapToObj(entries::get).forEach(printStream::println);
				}

			}
		});

	}

	private static class PerformanceListener implements ProgressListener {

		public final Instant startTime = Instant.now();

		private @Nullable Phase currentPhase = null;

		private final Map<Phase, Instant> phaseStarts = new HashMap<>();
		private final Map<Phase, Instant> phaseEnds = new HashMap<>();

		public Instant getPhaseStart(Phase phase) {
			if (!phaseStarts.containsKey(phase)) throw new IllegalStateException();
			return phaseStarts.get(phase);
		}

		public Instant getPhaseEnd(Phase phase) {
			if (!phaseEnds.containsKey(phase)) throw new IllegalStateException();
			return phaseEnds.get(phase);
		}

		public Duration getPhaseDuration(Phase phase) {
			return Duration.between(getPhaseStart(phase), getPhaseEnd(phase));
		}

		public Map<Phase, Duration> getPhaseDurations() {
			Map<Phase, Duration> durations = new HashMap<>();
			for (Phase phase : phaseStarts.keySet()) {
				if (phase != FINISHED) {
					durations.put(phase, getPhaseDuration(phase));
				}
			}
			return durations;
		}

		@Override
		public void updateProgress(Phase phase, double progress) {

			if (phase == currentPhase) return;

			phaseStarts.put(phase, now());

			if (currentPhase != null) {
				phaseEnds.put(currentPhase, now());
			}

			currentPhase = phase;

		}

	}

}
