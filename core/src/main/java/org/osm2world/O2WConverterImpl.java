package org.osm2world;

import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparingDouble;
import static java.util.Objects.requireNonNullElse;
import static org.osm2world.conversion.ConversionLog.LogLevel.FATAL;
import static org.osm2world.conversion.ProgressListener.Phase.FINISHED;
import static org.osm2world.conversion.ProgressListener.Phase.values;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bbox;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.osm2world.conversion.ConfigUtil;
import org.osm2world.conversion.ConversionLog;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.conversion.ProgressListener;
import org.osm2world.map_data.creation.OSMToMapDataConverter;
import org.osm2world.map_data.data.MapData;
import org.osm2world.map_elevation.creation.*;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.datastructures.IndexGrid;
import org.osm2world.math.datastructures.SpatialIndex;
import org.osm2world.math.geo.GeoBounds;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.geo.TileNumber;
import org.osm2world.math.shapes.FlatSimplePolygonShapeXYZ;
import org.osm2world.osm.creation.OSMDataReader;
import org.osm2world.osm.creation.OSMDataReaderView;
import org.osm2world.osm.data.OSMData;
import org.osm2world.output.Output;
import org.osm2world.output.common.compression.Compression;
import org.osm2world.output.common.compression.CompressionUtil;
import org.osm2world.scene.Scene;
import org.osm2world.scene.material.Materials;
import org.osm2world.scene.model.Models;
import org.osm2world.util.FaultTolerantIterationUtil;
import org.osm2world.world.attachment.AttachmentConnector;
import org.osm2world.world.attachment.AttachmentSurface;
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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;

import de.topobyte.osm4j.core.resolve.EntityNotFoundException;

/**
 * Implementation of {@link O2WConverter}.
 * Provides an easy way to call all steps of the conversion process in the correct order.
 */
class O2WConverterImpl {

	private final O2WConfig config;
	private final List<ProgressListener> listeners;

	O2WConverterImpl(@Nullable O2WConfig config, List<ProgressListener> listeners) {
		this.config = requireNonNullElse(config, new O2WConfig());
		this.listeners = new ArrayList<>(listeners);
	}

	/**
	 * implementation of {@link O2WConverter#convert(OSMDataReader, GeoBounds, MapProjection, Output...)}
	 */
	Scene convert(OSMDataReader osmDataReader, GeoBounds bounds, MapProjection mapProjection, Output[] outputs)
			throws IOException{

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

	/**
	 * implementation of {@link O2WConverter#convert(MapData, MapProjection, Output...)}
	 */
	Scene convert(MapData mapData, @Nullable MapProjection mapProjection, Output[] outputs) {

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

		ConfigUtil.parseFonts(config);
		Materials.configureMaterials(config);
		Models.configureModels(config);
		//this will cause problems if multiple conversions are run
		//at the same time, because global variables are being modified

		WorldCreator moduleManager = new WorldCreator(config, createModuleList(config));
		moduleManager.addRepresentationsTo(mapData);

		/* determine elevations */
		updatePhase(perfListener, ProgressListener.Phase.ELEVATION);

		TerrainElevationData eleData = null;

		if (config.srtmDir() != null) {
			if (mapProjection == null) {
				throw new IllegalArgumentException("Using SRTM data requires a map projection");
			}
			eleData = new SRTMData(config.srtmDir(), mapProjection);
		}

		/* create terrain and attach connectors */
		updatePhase(perfListener, ProgressListener.Phase.TERRAIN);

		calculateElevations(mapData, eleData, config);
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

		List<String> excludedModules = config.getList("excludeWorldModule")
				.stream().map(Object::toString).toList();

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

					List<AttachmentSurface> compatibleSurfaces = Streams.stream(nearbySurfaces)
							.filter(s -> s.getTypes().stream().anyMatch(t -> t.equals(surfaceType)))
							.filter(s -> s.getFaces().stream().anyMatch(f -> connector.isAcceptableNormal.test(f.getNormal())))
							.toList();

					Optional<AttachmentSurface> candidateSurface = Optional.empty();

					if ("roof".equals(surfaceType)) {
						// prioritize the topmost roof to avoid attaching to the hidden "roofs" of lower building parts
						double minDistanceXZ = compatibleSurfaces.stream()
								.mapToDouble(s -> s.distanceToXZ(connector.originalPos)).min().orElse(0);
						candidateSurface = compatibleSurfaces.stream()
								.filter(s -> s.distanceToXZ(connector.originalPos) < minDistanceXZ + 0.1)
								.max(comparingDouble(s -> s.closestPoint(connector.originalPos).y));
					} else {
						// choose the closest surface by 3D distance
						candidateSurface = compatibleSurfaces.stream()
								.min(comparingDouble(s -> s.distanceTo(connector.originalPos)));
					}

					if (candidateSurface.isPresent()) {
						attachConnectorIfValid(connector, candidateSurface.get());
						break;
					}
				}

			}

		}

	}

	static void attachConnectorIfValid(AttachmentConnector connector, AttachmentSurface surface) {

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

			VectorXYZ closestPoint;

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

		TerrainInterpolator interpolator = config.terrainInterpolator().get();

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

		try (FileWriter writer = new FileWriter(logDir.toPath().resolve(fileNameBase + ".json").toFile())) {

			Map<String, Object> jsonRoot = Map.of(
					"startTime", perfListener.startTime.toString(),
					"totalTime", totalTime,
					"timePerPhase", timePerPhase
			);

			new GsonBuilder().setPrettyPrinting().create().toJson(jsonRoot, writer);

		} catch (JsonIOException | IOException e) {
			throw new RuntimeException(e);
		}

		/* write a gz-compressed text file with the error log */

		Compression compression = Compression.GZ;
		File outputFile = logDir.toPath().resolve(fileNameBase + ".txt.gz").toFile();
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
