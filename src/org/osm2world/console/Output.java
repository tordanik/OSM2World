package org.osm2world.console;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.osm2world.console.CLIArgumentsUtil.OutputMode;
import org.osm2world.core.ConversionFacade;
import org.osm2world.core.ConversionFacade.Phase;
import org.osm2world.core.ConversionFacade.ProgressListener;
import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.LPEleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.LeastSquaresInterpolator;
import org.osm2world.core.map_elevation.creation.NaturalNeighborInterpolator;
import org.osm2world.core.map_elevation.creation.NoneEleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.SimpleEleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.TerrainInterpolator;
import org.osm2world.core.map_elevation.creation.ZeroInterpolator;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil.CardinalDirection;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.obj.ObjWriter;
import org.osm2world.core.target.povray.POVRayWriter;
import org.osm2world.core.util.functions.DefaultFactory;

public final class Output {

	private Output() {}

	public static void output(Configuration config,
			CLIArgumentsGroup argumentsGroup)
		throws IOException {
		
		long start = System.currentTimeMillis();
		
		ConversionFacade cf = new ConversionFacade();
		PerformanceListener perfListener =
			new PerformanceListener(argumentsGroup.getRepresentative());
		cf.addProgressListener(perfListener);
				
		String interpolatorType = config.getString("terrainInterpolator");
		if ("ZeroInterpolator".equals(interpolatorType)) {
			cf.setTerrainEleInterpolatorFactory(
					new DefaultFactory<TerrainInterpolator>(ZeroInterpolator.class));
		} else if ("LeastSquaresInterpolator".equals(interpolatorType)) {
			cf.setTerrainEleInterpolatorFactory(
					new DefaultFactory<TerrainInterpolator>(LeastSquaresInterpolator.class));
		} else if ("NaturalNeighborInterpolator".equals(interpolatorType)) {
			cf.setTerrainEleInterpolatorFactory(
					new DefaultFactory<TerrainInterpolator>(NaturalNeighborInterpolator.class));
		}
		
		String enforcerType = config.getString("eleConstraintEnforcer");
		if ("NoneEleConstraintEnforcer".equals(enforcerType)) {
			cf.setEleConstraintEnforcerFactory(
					new DefaultFactory<EleConstraintEnforcer>(NoneEleConstraintEnforcer.class));
		} else if ("SimpleEleConstraintEnforcer".equals(enforcerType)) {
			cf.setEleConstraintEnforcerFactory(
					new DefaultFactory<EleConstraintEnforcer>(SimpleEleConstraintEnforcer.class));
		} else if ("LPEleConstraintEnforcer".equals(enforcerType)) {
			cf.setEleConstraintEnforcerFactory(
					new DefaultFactory<EleConstraintEnforcer>(LPEleConstraintEnforcer.class));
		}
		
		Results results = cf.createRepresentations(
				argumentsGroup.getRepresentative().getInput(), null, config, null);
		
		ImageExporter exporter = null;
		
		for (CLIArguments args : argumentsGroup.getCLIArgumentsList()) {
			
			Camera camera = null;
			Projection projection = null;
			
			if (args.isOviewTiles()) {
				
				camera = OrthoTilesUtil.cameraForTiles(
						results.getMapProjection(),
						args.getOviewTiles(),
						args.getOviewAngle(),
						args.getOviewFrom());
				projection = OrthoTilesUtil.projectionForTiles(
						results.getMapProjection(),
						args.getOviewTiles(),
						args.getOviewAngle(),
						args.getOviewFrom());
				
			} else if (args.isOviewBoundingBox()) {
				
				double angle = args.getOviewAngle();
				CardinalDirection from = args.getOviewFrom();
				
				Collection<VectorXZ> pointsXZ = new ArrayList<VectorXZ>();
				for (LatLonEle l : args.getOviewBoundingBox()) {
					pointsXZ.add(results.getMapProjection().calcPos(l.lat, l.lon));
				}
				AxisAlignedBoundingBoxXZ bounds =
					new AxisAlignedBoundingBoxXZ(pointsXZ);
							
				camera = OrthoTilesUtil.cameraForBounds(bounds, angle, from);
				projection = OrthoTilesUtil.projectionForBounds(bounds, angle, from);
				
			} else if (args.isPviewPos()) {
				
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
							
			}
			
			for (File outputFile : args.getOutput()) {
				
				OutputMode outputMode =
					CLIArgumentsUtil.getOutputMode(outputFile);
				
				switch (outputMode) {
	
				case OBJ:
					Integer primitiveThresholdOBJ =
						config.getInteger("primitiveThresholdOBJ", null);
					if (primitiveThresholdOBJ == null) {
						ObjWriter.writeObjFile(outputFile,
								results.getMapData(), results.getMapProjection(),
								camera, projection);
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
			PrintWriter w = new PrintWriter(new FileWriter(
					argumentsGroup.getRepresentative().getPerformanceTable(), true), true);
			w.printf("|%6d |%6d |%6d |%6d |%6d |%6d |\n",
				(perfListener.getPhaseDuration(Phase.MAP_DATA) + 500) / 1000,
				(perfListener.getPhaseDuration(Phase.REPRESENTATION) + 500) / 1000,
				(perfListener.getPhaseDuration(Phase.ELEVATION) + 500) / 1000,
				(perfListener.getPhaseDuration(Phase.TERRAIN) + 500) / 1000,
				(System.currentTimeMillis() - perfListener.getPhaseEnd(Phase.TERRAIN) + 500) / 1000,
				(System.currentTimeMillis() - start + 500) / 1000);
			w.close();
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
