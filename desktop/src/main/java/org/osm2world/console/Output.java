package org.osm2world.console;

import static java.lang.Math.*;
import static java.time.Instant.now;
import static java.util.Map.entry;
import static java.util.stream.Collectors.toList;
import static org.osm2world.console.CLIArgumentsUtil.getOutputMode;
import static org.osm2world.console.CLIArgumentsUtil.getResolution;
import static org.osm2world.conversion.ConversionLog.LogLevel.FATAL;
import static org.osm2world.conversion.ProgressListener.Phase.*;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bbox;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import org.imintel.mbtiles4j.MBTilesReadException;
import org.osm2world.O2WConverter;
import org.osm2world.console.CLIArgumentsUtil.OutputMode;
import org.osm2world.conversion.ConversionLog;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.conversion.ProgressListener;
import org.osm2world.conversion.ProgressListener.Phase;
import org.osm2world.map_data.data.MapMetadata;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.geo.CardinalDirection;
import org.osm2world.math.geo.LatLonEle;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.osm.creation.OSMDataReaderView;
import org.osm2world.output.common.compression.Compression;
import org.osm2world.output.common.compression.CompressionUtil;
import org.osm2world.output.common.rendering.MutableCamera;
import org.osm2world.output.common.rendering.OrthographicUtil;
import org.osm2world.output.common.rendering.PerspectiveProjection;
import org.osm2world.output.common.rendering.Projection;
import org.osm2world.output.frontend_pbf.FrontendPbfOutput;
import org.osm2world.output.gltf.GltfOutput;
import org.osm2world.output.image.ImageExporter;
import org.osm2world.output.image.ImageOutputFormat;
import org.osm2world.output.obj.ObjMultiFileOutput;
import org.osm2world.output.obj.ObjOutput;
import org.osm2world.output.povray.POVRayOutput;
import org.osm2world.scene.Scene;
import org.osm2world.util.Resolution;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;

final class Output {

	private Output() {}

	public static void output(O2WConfig config,
			CLIArgumentsGroup argumentsGroup)
		throws IOException {

		CLIArguments sharedArgs = argumentsGroup.getRepresentative();

		var perfListener = new PerformanceListener();

		if (sharedArgs.isLogDir()) {
			ConversionLog.setConsoleLogLevels(EnumSet.of(ConversionLog.LogLevel.FATAL));
		}

		try {

			MapMetadata metadata = null;

			if (sharedArgs.isMetadataFile()) {
				if (sharedArgs.getMetadataFile().getName().endsWith(".mbtiles")) {
					if (sharedArgs.isMetadataFile() && sharedArgs.isTile()) {
						try {
							metadata = MapMetadata.metadataForTile(sharedArgs.getTile(), sharedArgs.getMetadataFile());
						} catch (MBTilesReadException e) {
							System.err.println("Cannot read tile metadata: " + e);
						}
					}
				} else {
					metadata = MapMetadata.metadataFromJson(sharedArgs.getMetadataFile());
				}
			}

			if (metadata != null && metadata.land() == Boolean.FALSE) {
				config = config.withProperty("isAtSea", true);
			}

			var converter = new O2WConverter();
			converter.setConfig(config);
			converter.addProgressListener(perfListener);

			OSMDataReaderView osmReaderView = CLIArgumentsUtil.getOsmDataView(sharedArgs);

			Scene scene = converter.convert(osmReaderView, null, null);

			ImageExporter exporter = null;

			for (CLIArguments args : argumentsGroup.getCLIArgumentsList()) {

				/* set camera and projection */

				MutableCamera camera;
				Projection projection;

				if (args.isPviewPos()) {

					/* perspective projection */

					MapProjection proj = scene.getMapProjection();

					LatLonEle pos = args.getPviewPos();
					LatLonEle lookAt = args.getPviewLookat();

					camera = new MutableCamera();
					VectorXYZ posXYZ = proj.toXZ(pos.lat, pos.lon).xyz(pos.ele);
					VectorXYZ lookAtXYZ = proj.toXZ(lookAt.lat, lookAt.lon).xyz(lookAt.ele);
					camera.setCamera(posXYZ, lookAtXYZ);

					projection = new PerspectiveProjection(
							args.isPviewAspect() ? args.getPviewAspect() :
									args.isResolution() ? (double) args.getResolution().getAspectRatio()
											: CLIArgumentsUtil.DEFAULT_ASPECT_RATIO,
							args.getPviewFovy(),
							1, 50000);

				} else {

					/* orthographic projection */

					double angle = args.getOviewAngle();
					CardinalDirection from = args.getOviewFrom();

					AxisAlignedRectangleXZ bounds;

					if (args.isOviewBoundingBox()) {
						bounds = bbox(args.getOviewBoundingBox().stream()
								.map(scene.getMapProjection()::toXZ)
								.collect(toList()));
					} else if (args.isOviewTiles()) {
						bounds = OrthographicUtil.boundsForTiles(scene.getMapProjection(), args.getOviewTiles());
					} else if (args.isTile()) {
						bounds = OrthographicUtil.boundsForTile(scene.getMapProjection(), args.getTile());
					} else {
						bounds = scene.getBoundary();
					}

					camera = OrthographicUtil.cameraForBounds(bounds, angle, from);
					projection = OrthographicUtil.projectionForBounds(bounds, angle, from);

				}

				/* perform the actual output */

				boolean underground = config.getBoolean("renderUnderground", true);

				for (File outputFile : args.getOutput()) {

					outputFile.getAbsoluteFile().getParentFile().mkdirs();

					OutputMode outputMode = CLIArgumentsUtil.getOutputMode(outputFile);

					switch (outputMode) {

						case OBJ: {
							Integer primitiveThresholdOBJ = config.getInteger("primitiveThresholdOBJ", null);
							var output = (primitiveThresholdOBJ == null)
									? new ObjOutput(outputFile, scene.getMapProjection())
									: new ObjMultiFileOutput(outputFile, scene.getMapProjection(), primitiveThresholdOBJ);
							output.setConfiguration(config);
							output.outputScene(scene);
						}
						break;

						case GLTF, GLB, GLTF_GZ, GLB_GZ: {
							AxisAlignedRectangleXZ bounds;
							if (args.isTile()) {
								bounds = OrthographicUtil.boundsForTile(scene.getMapProjection(), args.getTile());
							} else {
								bounds = scene.getBoundary();
							}
							GltfOutput.GltfFlavor gltfFlavor = EnumSet.of(OutputMode.GLB, OutputMode.GLB_GZ).contains(outputMode)
									? GltfOutput.GltfFlavor.GLB : GltfOutput.GltfFlavor.GLTF;
							Compression compression = EnumSet.of(OutputMode.GLTF_GZ, OutputMode.GLB_GZ).contains(outputMode)
									? Compression.GZ : Compression.NONE;
							GltfOutput output = new GltfOutput(outputFile, gltfFlavor, compression, bounds);
							output.setConfiguration(config);
							output.outputScene(scene);
						}
						break;

						case POV: {
							POVRayOutput output = new POVRayOutput(outputFile, camera, projection);
							output.setConfiguration(config);
							output.outputScene(scene);
						}
						break;

						case WEB_PBF, WEB_PBF_GZ: {
							AxisAlignedRectangleXZ bbox;
							if (args.isTile()) {
								bbox = OrthographicUtil.boundsForTile(scene.getMapProjection(), args.getTile());
							} else {
								bbox = scene.getBoundary();
							}
							Compression compression = outputMode == OutputMode.WEB_PBF_GZ ? Compression.GZ : Compression.NONE;
							FrontendPbfOutput output = new FrontendPbfOutput(outputFile, compression, bbox);
							output.setConfiguration(config);
							output.outputScene(scene);
						}
						break;

						case PNG:
						case PPM:
						case GD:
							if (camera == null || projection == null) {
								System.err.println("camera or projection missing");
							}
							if (exporter == null) {
								PerformanceParams performanceParams = determinePerformanceParams(config, argumentsGroup);
								exporter = ImageExporter.create(config, scene.getBoundary(),
										output -> output.outputScene(scene),
										performanceParams.resolution(), performanceParams.unbufferedRendering());
							}
							Resolution resolution = CLIArgumentsUtil.getResolution(args);
							ImageOutputFormat imageFormat = switch (outputMode) {
								case PNG -> ImageOutputFormat.PNG;
								case PPM -> ImageOutputFormat.PPM;
								case GD -> ImageOutputFormat.GD;
								default -> throw new IllegalStateException("Not an image format: " + outputMode);
							};
							exporter.writeImageFile(outputFile, imageFormat,
									resolution.width, resolution.height,
									camera, projection);
							break;

					}

				}

			}

			if (exporter != null) {
				exporter.freeResources();
				exporter = null;
			}

		} catch (Exception e) {
			ConversionLog.log(FATAL, "Conversion failed", e, null);
			throw e;
		} finally {

			if (sharedArgs.isLogDir()) {

				File logDir = sharedArgs.getLogDir();
				logDir.mkdirs();

				String fileNameBase = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm_ss"));
				if (sharedArgs.isTile()) {
					fileNameBase = sharedArgs.getTile().toString("_");
				}
				fileNameBase = "osm2world_log_" + fileNameBase;

				writeLogFiles(logDir, fileNameBase, perfListener, config);

			}

		}

	}

	/** parameters for optimizing the performance of an {@link ImageExporter} */
	private record PerformanceParams(Resolution resolution, boolean unbufferedRendering) {

		public PerformanceParams (int pBufferSizeX, int pBufferSizeY, boolean unbufferedRendering) {
			this(new Resolution(pBufferSizeX, pBufferSizeY), unbufferedRendering);
		}

	}

	/**
	 * Optimizes an {@link ImageExporter}'s performance settings for use with a particular config
	 * and a particular group of files, based on a {@link CLIArgumentsGroup}.
	 *
	 * @param expectedGroup  group that should contain at least the arguments
	 *                       for the files that will later be requested.
	 *                       Basis for optimization preparations.
	 */
	private static PerformanceParams determinePerformanceParams(O2WConfig config, CLIArgumentsGroup expectedGroup) {

		int canvasLimit = config.canvasLimit();

		/* find out what number and size of image file requests to expect */

		int expectedFileCalls = 0;
		int expectedMaxSizeX = 1;
		int expectedMaxSizeY = 1;
		boolean perspectiveProjection = false;

		for (CLIArguments args : expectedGroup.getCLIArgumentsList()) {

			for (File outputFile : args.getOutput()) {
				CLIArgumentsUtil.OutputMode outputMode = getOutputMode(outputFile);
				if (outputMode == CLIArgumentsUtil.OutputMode.PNG || outputMode == CLIArgumentsUtil.OutputMode.PPM || outputMode == CLIArgumentsUtil.OutputMode.GD) {
					expectedFileCalls += 1;
					expectedMaxSizeX = max(expectedMaxSizeX, getResolution(args).width);
					expectedMaxSizeY = max(expectedMaxSizeY, getResolution(args).height);
					perspectiveProjection |= args.isPviewPos();
				}
			}

		}

		boolean onlyOneRenderPass = (expectedFileCalls <= 1
				&& expectedMaxSizeX <= canvasLimit
				&& expectedMaxSizeY <= canvasLimit);

		/* call the constructor */

		boolean unbufferedRendering = onlyOneRenderPass
				|| config.getBoolean("forceUnbufferedPNGRendering", false);

		int pBufferSizeX, pBufferSizeY;

		if (perspectiveProjection) {
			pBufferSizeX = expectedMaxSizeX;
			pBufferSizeY = expectedMaxSizeY;
		} else {
			pBufferSizeX = min(canvasLimit, expectedMaxSizeX);
			pBufferSizeY = min(canvasLimit, expectedMaxSizeY);
		}

		return new PerformanceParams(pBufferSizeX, pBufferSizeY, unbufferedRendering);

	}

	private static void writeLogFiles(File logDir, String fileNameBase, PerformanceListener perfListener,
			O2WConfig config) {

		double totalTime = Duration.between(perfListener.startTime, now()).toMillis() / 1000.0;

		Map<Phase, Double> timePerPhase;
		if (perfListener.currentPhase == FINISHED) {
			timePerPhase = Map.ofEntries(
					entry(MAP_DATA, perfListener.getPhaseDuration(MAP_DATA).toMillis() / 1000.0),
					entry(REPRESENTATION, perfListener.getPhaseDuration(REPRESENTATION).toMillis() / 1000.0),
					entry(ELEVATION, perfListener.getPhaseDuration(ELEVATION).toMillis() / 1000.0),
					entry(TERRAIN, perfListener.getPhaseDuration(TERRAIN).toMillis() / 1000.0),
					entry(OUTPUT, Duration.between(perfListener.getPhaseEnd(TERRAIN), now()).toMillis() / 1000.0)
			);
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
					for (Phase phase : ProgressListener.Phase.values()) {
						if (timePerPhase.containsKey(phase)) {
							printStream.println(phase + ": " + timePerPhase.get(phase));
						}
					}
				}

				printStream.println();

				List<ConversionLog.Entry> entries = ConversionLog.getLog();
				int maxLogEntries = config.getInt("maxLogEntries", 100);

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
