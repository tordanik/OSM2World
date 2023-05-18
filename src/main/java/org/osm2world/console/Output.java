package org.osm2world.console;

import static java.time.Instant.now;
import static java.util.Collections.singletonList;
import static java.util.Map.entry;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.ConversionFacade.Phase.*;
import static org.osm2world.core.math.AxisAlignedRectangleXZ.bbox;

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
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.configuration.Configuration;
import org.osm2world.console.CLIArgumentsUtil.OutputMode;
import org.osm2world.core.ConversionFacade;
import org.osm2world.core.ConversionFacade.Phase;
import org.osm2world.core.ConversionFacade.ProgressListener;
import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.conversion.ConversionLog;
import org.osm2world.core.map_data.creation.LatLonBounds;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.map_elevation.creation.*;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.osm.creation.*;
import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil.CardinalDirection;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.frontend_pbf.FrontendPbfTarget;
import org.osm2world.core.target.gltf.GltfTarget;
import org.osm2world.core.target.obj.ObjWriter;
import org.osm2world.core.target.povray.POVRayWriter;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;

public final class Output {

	private Output() {}

	public static void output(Configuration config,
			CLIArgumentsGroup argumentsGroup)
		throws IOException {

		var perfListener = new PerformanceListener(argumentsGroup.getRepresentative().getPerformancePrint());

		if (argumentsGroup.getRepresentative().isLogDir()) {
			ConversionLog.setConsoleLogLevels(EnumSet.of(ConversionLog.LogLevel.FATAL));
		}

		OSMDataReader dataReader = null;

		switch (argumentsGroup.getRepresentative().getInputMode()) {

		case FILE:
			File inputFile = argumentsGroup.getRepresentative().getInput();
			dataReader = switch (CLIArgumentsUtil.getInputFileType(argumentsGroup.getRepresentative())) {
				case SIMPLE_FILE -> new OSMFileReader(inputFile);
				case MBTILES -> new MbtilesReader(inputFile, argumentsGroup.getRepresentative().getTile());
				case GEODESK -> new GeodeskReader(inputFile, argumentsGroup.getRepresentative().getTile().bounds());
			};
			break;

		case OVERPASS:
			if (argumentsGroup.getRepresentative().isInputBoundingBox()) {
				LatLonBounds bounds = LatLonBounds.ofPoints(argumentsGroup.getRepresentative().getInputBoundingBox());
				dataReader = new OverpassReader(argumentsGroup.getRepresentative().getOverpassURL(), bounds);
			} else if (argumentsGroup.getRepresentative().isTile()) {
				LatLonBounds bounds = argumentsGroup.getRepresentative().getTile().bounds();
				dataReader = new OverpassReader(argumentsGroup.getRepresentative().getOverpassURL(), bounds);
			} else {
				assert argumentsGroup.getRepresentative().isInputQuery(); // can be assumed due to input validation
				String query = argumentsGroup.getRepresentative().getInputQuery();
				dataReader = new OverpassReader(argumentsGroup.getRepresentative().getOverpassURL(), query);
			}
			break;

		}


		ConversionFacade cf = new ConversionFacade();
		cf.addProgressListener(perfListener);

		String interpolatorType = config.getString("terrainInterpolator");
		if ("ZeroInterpolator".equals(interpolatorType)) {
			cf.setTerrainEleInterpolatorFactory(ZeroInterpolator::new);
		} else if ("LeastSquaresInterpolator".equals(interpolatorType)) {
			cf.setTerrainEleInterpolatorFactory(LeastSquaresInterpolator::new);
		} else if ("NaturalNeighborInterpolator".equals(interpolatorType)) {
			cf.setTerrainEleInterpolatorFactory(NaturalNeighborInterpolator::new);
		}

		String enforcerType = config.getString("eleConstraintEnforcer");
		if ("NoneEleConstraintEnforcer".equals(enforcerType)) {
			cf.setEleConstraintEnforcerFactory(NoneEleConstraintEnforcer::new);
		} else if ("SimpleEleConstraintEnforcer".equals(enforcerType)) {
			cf.setEleConstraintEnforcerFactory(SimpleEleConstraintEnforcer::new);
		}

		Results results = cf.createRepresentations(dataReader.getData(), null, config, null);

		ImageExporter exporter = null;

		for (CLIArguments args : argumentsGroup.getCLIArgumentsList()) {

			/* set camera and projection */

			Camera camera = null;
			Projection projection = null;

			if (args.isPviewPos()) {

				/* perspective projection */

				MapProjection proj = results.getMapProjection();

				LatLonEle pos = args.getPviewPos();
				LatLonEle lookAt = args.getPviewLookat();

				camera = new Camera();
				VectorXYZ posV = proj.toXZ(pos.lat, pos.lon).xyz(pos.ele);
				VectorXYZ laV =	proj.toXZ(lookAt.lat, lookAt.lon).xyz(lookAt.ele);
				camera.setCamera(posV.x, posV.y, posV.z, laV.x, laV.y, laV.z);

				projection = new Projection(false,
						args.isPviewAspect() ? args.getPviewAspect() :
							(double)args.getResolution().getAspectRatio(),
							args.getPviewFovy(),
						0,
						1, 50000);

			} else {

				/* orthographic projection */

				double angle = args.getOviewAngle();
				CardinalDirection from = args.getOviewFrom();

				AxisAlignedRectangleXZ bounds;

				if (args.isOviewBoundingBox()) {
					bounds = bbox(args.getOviewBoundingBox().stream()
							.map(results.getMapProjection()::toXZ)
							.collect(toList()));
				} else if (args.isOviewTiles()) {
					bounds = OrthoTilesUtil.boundsForTiles(results.getMapProjection(), args.getOviewTiles());
				} else if (args.isTile()) {
					bounds = OrthoTilesUtil.boundsForTiles(results.getMapProjection(), singletonList(args.getTile()));
				} else {
					bounds = results.getMapData().getBoundary();
				}

				camera = OrthoTilesUtil.cameraForBounds(bounds, angle, from);
				projection = OrthoTilesUtil.projectionForBounds(bounds, angle, from);

			}

			/* perform the actual output */

			for (File outputFile : args.getOutput()) {

				outputFile.getAbsoluteFile().getParentFile().mkdirs();

				OutputMode outputMode = CLIArgumentsUtil.getOutputMode(outputFile);

				switch (outputMode) {

				case OBJ:
					Integer primitiveThresholdOBJ =
						config.getInteger("primitiveThresholdOBJ", null);
					if (primitiveThresholdOBJ == null) {
						boolean underground = config.getBoolean("renderUnderground", true);

						ObjWriter.writeObjFile(outputFile,
								results.getMapData(), results.getMapProjection(), config,
								camera, projection, underground);
					} else {
						ObjWriter.writeObjFiles(outputFile,
								results.getMapData(), results.getMapProjection(), config,
								camera, projection, primitiveThresholdOBJ);
					}
					break;

				case GLTF:
					AxisAlignedRectangleXZ bounds = null;
					if (args.isTile()) {
						bounds = OrthoTilesUtil.boundsForTiles(results.getMapProjection(), singletonList(args.getTile()));
					} else {
						bounds = results.getMapData().getBoundary();
					}
					GltfTarget gltfTarget = new GltfTarget(outputFile, bounds);
					gltfTarget.setConfiguration(config);
					boolean underground = config.getBoolean("renderUnderground", true);
					TargetUtil.renderWorldObjects(gltfTarget, results.getMapData(), underground);
					gltfTarget.finish();
					break;

				case POV:
					POVRayWriter.writePOVInstructionFile(outputFile,
							results.getMapData(), camera, projection);
					break;

				case WEB_PBF:
					AxisAlignedRectangleXZ bbox = null;
					if (args.isTile()) {
						bbox = OrthoTilesUtil.boundsForTiles(results.getMapProjection(), singletonList(args.getTile()));
					}
					FrontendPbfTarget.writePbfFile(
							outputFile, results.getMapData(), bbox, results.getMapProjection());
					break;

				case PNG:
				case PPM:
				case GD:
					if (camera == null || projection == null) {
						System.err.println("camera or projection missing");
					}
					if (exporter == null) {
						exporter = new ImageExporter(
								config, results, argumentsGroup);
					}
					exporter.writeImageFile(outputFile, outputMode,
							args.getResolution().width, args.getResolution().height,
							camera, projection);
					break;

				}

			}

		}

		if (exporter != null) {
			exporter.freeResources();
			exporter = null;
		}

		if (argumentsGroup.getRepresentative().getPerformancePrint()) {
			long timeSec = Duration.between(perfListener.startTime, now()).getSeconds();
			System.out.println("finished after " + timeSec + " s");
		}

		if (argumentsGroup.getRepresentative().isLogDir()) {

			File logDir = argumentsGroup.getRepresentative().getLogDir();
			logDir.mkdirs();

			String fileNameBase = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm_ss"));
			if (argumentsGroup.getRepresentative().isTile()) {
				fileNameBase = argumentsGroup.getRepresentative().getTile().toString("_");
			}
			fileNameBase = "osm2world_log_" + fileNameBase;

			writeLogFiles(logDir, fileNameBase, perfListener);

		}

	}

	private static void writeLogFiles(File logDir, String fileNameBase, PerformanceListener perfListener) {

		double totalTime = Duration.between(perfListener.startTime, now()).toMillis() / 1000.0;
		Map<Phase, Double> timePerPhase = Map.ofEntries(
				entry(MAP_DATA, perfListener.getPhaseDuration(MAP_DATA).toMillis() / 1000.0),
				entry(REPRESENTATION, perfListener.getPhaseDuration(REPRESENTATION).toMillis() / 1000.0),
				entry(ELEVATION, perfListener.getPhaseDuration(ELEVATION).toMillis() / 1000.0),
				entry(TERRAIN, perfListener.getPhaseDuration(TERRAIN).toMillis() / 1000.0),
				entry(TARGET, Duration.between(perfListener.getPhaseEnd(TERRAIN), now()).toMillis() / 1000.0)
		);

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

		/* write a text file with the error log */

		try (var printStream = new PrintStream(logDir.toPath().resolve(fileNameBase + ".txt").toFile())) {
			printStream.println("Runtime (seconds):\nTotal: " + totalTime);
			for (Phase phase : Phase.values()) {
				if (timePerPhase.containsKey(phase)) {
					printStream.println(phase + ": " + timePerPhase.get(phase));
				}
			}
			printStream.println();
			ConversionLog.getLog().forEach(printStream::println);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	private static class PerformanceListener implements ProgressListener {

		public final Instant startTime = Instant.now();
		private final boolean printToSysout;

		public PerformanceListener(boolean printToSysout) {
			this.printToSysout = printToSysout;
		}

		private @Nullable Phase currentPhase = null;
		private @Nullable Instant currentPhaseStart;

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
		public void updatePhase(Phase newPhase) {

			phaseStarts.put(newPhase, now());

			if (currentPhase != null) {

				phaseEnds.put(currentPhase, now());

				if (printToSysout) {
					long ms = Duration.between(currentPhaseStart, now()).toMillis();
					System.out.println("phase " + currentPhase + " finished after " + ms + " ms");
				}

			}

			currentPhase = newPhase;
			currentPhaseStart = now();

		}

	}

}
