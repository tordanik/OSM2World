package org.osm2world.console;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.osm2world.console.CLIArgumentsUtil.OutputMode;
import org.osm2world.core.ConversionFacade;
import org.osm2world.core.ConversionFacade.Phase;
import org.osm2world.core.ConversionFacade.ProgressListener;
import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.OrthoTilesUtil;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.target.obj.ObjWriter;
import org.osm2world.core.target.povray.POVRayWriter;

public final class Output {

	private Output() {}

	public static void output(CLIArguments args) throws IOException {
				
		long start = System.currentTimeMillis();
		
		ConversionFacade cf = new ConversionFacade();
		PerformanceListener perfListener = new PerformanceListener(args);
		cf.addProgressListener(perfListener);
		
		Configuration config = new BaseConfiguration();
		
		if (args.isConfig()) {
			try {
				config = new PropertiesConfiguration(args.getConfig());
			} catch (ConfigurationException e) {
				System.err.println("could not read config, ignoring it: ");
				System.err.println(e);
			}
		}
		
		Results results =
			cf.createRepresentations(args.getInput(), null, config, null);
		
		Camera camera = null;
		Projection projection = null;
		
		if (args.isOviewTiles()) {
			
			camera = OrthoTilesUtil.cameraForTiles(
					results.getMapProjection(),
					args.getOviewTiles(),
					args.getOviewAngle());
			projection = OrthoTilesUtil.projectionForTiles(
					results.getMapProjection(),
					args.getOviewTiles(),
					args.getOviewAngle());
			
		} else if (args.isOviewBoundingBox()) {
			
			double angle = args.getOviewAngle();
			
			Collection<VectorXZ> pointsXZ = new ArrayList<VectorXZ>();
			for (LatLonEle l : args.getOviewBoundingBox()) {
				pointsXZ.add(results.getMapProjection().calcPos(l.lat, l.lon));
			}
			AxisAlignedBoundingBoxXZ bounds =
				new AxisAlignedBoundingBoxXZ(pointsXZ);
						
			camera = OrthoTilesUtil.cameraForBounds(bounds, angle);
			projection = OrthoTilesUtil.projectionForBounds(bounds, angle);
			
		} else if (args.isPviewPos()) {
			
			MapProjection proj = results.getMapProjection();
			
			LatLonEle pos = args.getPviewPos();
			LatLonEle lookAt = args.getPviewLookat();
			
			camera = new Camera();
			camera.setPos(proj.calcPos(pos.lat, pos.lon).xyz(pos.ele));
			camera.setLookAt(proj.calcPos(lookAt.lat, lookAt.lon).xyz(lookAt.ele));
			
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
				ObjWriter.writeObjFile(outputFile,
						results.getMapData(), results.getEleData(),
						results.getTerrain(), results.getMapProjection(),
						camera, projection);
				break;
				
			case POV:
				POVRayWriter.writePOVInstructionFile(outputFile,
						results.getMapData(), results.getEleData(), results.getTerrain(),
						camera, projection);
				break;
				
			case PNG:
				if (camera == null || projection == null) {
					System.err.println("camera or projection missing");
				}
				ImageExport.writeImageFile(outputFile,
						args.getResolution().x, args.getResolution().y,
						results, camera, projection);
				break;
				
			}
			
		}
		
		if (args.getPerformancePrint()) {
			long timeSec = (System.currentTimeMillis() - start) / 1000;
			System.out.println("finished after " + timeSec + " s");
		}
		
		if (args.isPerformanceTable()) {
			PrintWriter w = new PrintWriter(new FileWriter(args.getPerformanceTable(), true), true);
			w.printf("|%6d |%6d |%6d |%6d |%6d |%6d |\n",
				(perfListener.getPhaseDuration(Phase.MAP_DATA) + 500) / 1000,
				(perfListener.getPhaseDuration(Phase.REPRESENTATION) + 500) / 1000,
				(perfListener.getPhaseDuration(Phase.ELEVATION) + 500) / 1000,
				(perfListener.getPhaseDuration(Phase.TERRAIN) + 500) / 1000,
				(System.currentTimeMillis() - perfListener.getPhaseEnd(Phase.TERRAIN) + 500) / 1000,
				(System.currentTimeMillis() - start + 500) / 1000);
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
