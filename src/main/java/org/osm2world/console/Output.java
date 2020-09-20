package org.osm2world.console;

import static java.lang.Double.*;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.AxisAlignedRectangleXZ.bbox;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.osm2world.console.CLIArgumentsUtil.OutputMode;
import org.osm2world.core.ConversionFacade;
import org.osm2world.core.ConversionFacade.Phase;
import org.osm2world.core.ConversionFacade.ProgressListener;
import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.map_data.creation.LatLon;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.map_elevation.creation.LeastSquaresInterpolator;
import org.osm2world.core.map_elevation.creation.NaturalNeighborInterpolator;
import org.osm2world.core.map_elevation.creation.NoneEleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.SimpleEleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.ZeroInterpolator;
import org.osm2world.core.math.AxisAlignedRectangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.osm.creation.MbtilesReader;
import org.osm2world.core.osm.creation.OSMDataReader;
import org.osm2world.core.osm.creation.OSMFileReader;
import org.osm2world.core.osm.creation.OverpassReader;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil.CardinalDirection;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.frontend_pbf.FrontendPbfTarget;
import org.osm2world.core.target.obj.ObjWriter;
import org.osm2world.core.target.povray.POVRayWriter;

public final class Output {

	private Output() {}

	public static void output(Configuration config,
			CLIArgumentsGroup argumentsGroup)
		throws IOException {

		long start = System.currentTimeMillis();

		OSMDataReader dataReader = null;

		switch (argumentsGroup.getRepresentative().getInputMode()) {

		case FILE:
			File inputFile = argumentsGroup.getRepresentative().getInput();
			if (inputFile.getName().endsWith(".mbtiles")) {
				dataReader = new MbtilesReader(inputFile, argumentsGroup.getRepresentative().getTile());
			} else {
				dataReader = new OSMFileReader(inputFile);
			}
			break;

		case OVERPASS:
			if (argumentsGroup.getRepresentative().isInputBoundingBox()) {

				double minLat = POSITIVE_INFINITY;
				double maxLat = NEGATIVE_INFINITY;
				double minLon = POSITIVE_INFINITY;
				double maxLon = NEGATIVE_INFINITY;

				for (LatLonEle l : argumentsGroup.getRepresentative().getInputBoundingBox()) {
					if (l.lat < minLat) {
						minLat = l.lat;
					}
					if (l.lat > maxLat) {
						maxLat = l.lat;
					}
					if (l.lon < minLon) {
						minLon = l.lon;
					}
					if (l.lon > maxLon) {
						maxLon = l.lon;
					}
				}

				dataReader = new OverpassReader(argumentsGroup.getRepresentative().getOverpassURL(),
						new LatLon(minLat, minLon), new LatLon(maxLat, maxLon));

			} else { //due to input validation, there needs to be either a query or bounding box for Overpass input mode
				assert argumentsGroup.getRepresentative().isInputQuery();
				dataReader = new OverpassReader(argumentsGroup.getRepresentative().getOverpassURL(),
						argumentsGroup.getRepresentative().getInputQuery());
			}
			break;

		}


		ConversionFacade cf = new ConversionFacade();
		PerformanceListener perfListener =
			new PerformanceListener(argumentsGroup.getRepresentative());
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
				VectorXYZ posV = proj.calcPos(pos.lat, pos.lon).xyz(pos.ele);
				VectorXYZ laV =	proj.calcPos(lookAt.lat, lookAt.lon).xyz(lookAt.ele);
				camera.setCamera(posV.x, posV.y, posV.z, laV.x, laV.y, laV.z);

				projection = new Projection(false,
						args.isPviewAspect() ? args.getPviewAspect() :
							(double)args.getResolution().x / args.getResolution().y,
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
							.map(LatLonEle::latLon)
							.map(results.getMapProjection()::calcPos)
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

				outputFile.getParentFile().mkdirs();

				OutputMode outputMode = CLIArgumentsUtil.getOutputMode(outputFile);

				switch (outputMode) {

				case OBJ:
					Integer primitiveThresholdOBJ =
						config.getInteger("primitiveThresholdOBJ", null);
					if (primitiveThresholdOBJ == null) {
						boolean underground = config.getBoolean("renderUnderground", true);

						ObjWriter.writeObjFile(outputFile,
								results.getMapData(), results.getMapProjection(),
								camera, projection, underground);
					} else {
						ObjWriter.writeObjFiles(outputFile,
								results.getMapData(), results.getMapProjection(),
								camera, projection, primitiveThresholdOBJ);
					}
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
							args.getResolution().x, args.getResolution().y,
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
			long timeSec = (System.currentTimeMillis() - start) / 1000;
			System.out.println("finished after " + timeSec + " s");
		}

		if (argumentsGroup.getRepresentative().isPerformanceTable()) {
			try (PrintWriter w = new PrintWriter(new FileWriter(
					argumentsGroup.getRepresentative().getPerformanceTable(), true), true)) {
				w.printf("|%6d |%6d |%6d |%6d |%6d |%6d |\n",
					(perfListener.getPhaseDuration(Phase.MAP_DATA) + 500) / 1000,
					(perfListener.getPhaseDuration(Phase.REPRESENTATION) + 500) / 1000,
					(perfListener.getPhaseDuration(Phase.ELEVATION) + 500) / 1000,
					(perfListener.getPhaseDuration(Phase.TERRAIN) + 500) / 1000,
					(System.currentTimeMillis() - perfListener.getPhaseEnd(Phase.TERRAIN) + 500) / 1000,
					(System.currentTimeMillis() - start + 500) / 1000);
			}
		}

	}

	private static class PerformanceListener implements ProgressListener {

		private final CLIArguments args;
		public PerformanceListener(CLIArguments args) {
			this.args = args;
		}

		private Phase currentPhase = null;
		private long currentPhaseStart;

		private Map<Phase, Long> phaseStarts = new HashMap<Phase, Long>();
		private Map<Phase, Long> phaseEnds = new HashMap<Phase, Long>();

		public Long getPhaseStart(Phase phase) {
			return phaseStarts.get(phase);
		}

		public Long getPhaseEnd(Phase phase) {
			return phaseEnds.get(phase);
		}

		public Long getPhaseDuration(Phase phase) {
			return getPhaseEnd(phase) - getPhaseStart(phase);
		}

		@Override
		public void updatePhase(Phase newPhase) {

			phaseStarts.put(newPhase, System.currentTimeMillis());

			if (currentPhase != null) {

				phaseEnds.put(currentPhase, System.currentTimeMillis());

				if (args.getPerformancePrint()) {
					long ms = System.currentTimeMillis() - currentPhaseStart;
					System.out.println("phase " + currentPhase
						+  " finished after " + ms + " ms");
				}

			}

			currentPhase = newPhase;
			currentPhaseStart = System.currentTimeMillis();

		}

	}

}
