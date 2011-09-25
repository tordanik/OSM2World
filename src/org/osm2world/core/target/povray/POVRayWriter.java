package org.osm2world.core.target.povray;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.osm2world.core.heightmap.data.CellularTerrainElevation;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.terrain.data.Terrain;

/**
 * utility class for creating a POVRay file
 */
public final class POVRayWriter {

	/** prevents instantiation */
	private POVRayWriter() { }
	
	public static final void writePOVInstructionFile(File file, MapData mapData,
			CellularTerrainElevation eleData, Terrain terrain,
			Camera camera, Projection projection)
			throws IOException {
		
		if (!file.exists()) {
			file.createNewFile();
		}
		
		PrintStream printStream = new PrintStream(file);
		
		writePOVInstructionStringToStream(printStream,
				mapData, eleData, terrain, camera, projection);
		
		printStream.close();
		
	}

	private static final void writePOVInstructionStringToStream(
			PrintStream stream, MapData mapData,
			CellularTerrainElevation eleData, Terrain terrain,
			Camera camera, Projection projection) {
				
		POVRayTarget target = new POVRayTarget(stream);

		addRenderParameters(target);
		
		target.append("\n#include \"textures.inc\"\n#include \"colors.inc\"\n");
		target.append("#include \"osm2world_definitions.inc\"\n\n");
				
		target.append("global_settings { ambient_light rgb 1 }\n");
		target.append("light_source{ <100000,150000,-100000> color White parallel point_at <0,0,0> fade_power 0 }\n\n");
		
		if (camera != null && projection != null) {
			addCameraDefinition(target, camera, projection);
		}
		
		target.append("#ifndef (sky_sphere_def)\n");
		target.append("#declare sky_sphere_def = sky_sphere {\n");
		target.append("  pigment { Blue_Sky3 }\n");
		target.append("}\n");
		target.append("#end\n\n");
		target.append("sky_sphere {sky_sphere_def}\n\n");
		
		target.appendMaterialDefinitions();
		
		target.append("//\n// empty ground around the scene\n//\n\n");
		
		target.append("difference {\n");
		target.append("  plane { y, -0.001 }\n  ");
		VectorXZ[] boundary = eleData.getBoundaryPolygon().getXZPolygon()
			.getVertexLoop().toArray(new VectorXZ[0]);
		target.appendPrism( -100, 1, boundary);
		target.append("\n");
		target.appendMaterialOrName(Materials.TERRAIN_DEFAULT);
		target.append("\n}\n\n");
		
		target.append("\n\n//\n//Map data\n//\n\n");
		
		TargetUtil.renderWorldObjects(target, mapData);

		target.append("\n\n//\n//Terrain\n//\n\n");
		
		terrain.renderTo(target);
		
	}
		
	private static final void addCameraDefinition(POVRayTarget target,
			Camera camera, Projection projection) {
		
		target.append("camera {");
		
		if (projection.isOrthographic()) {
			target.append("\n  orthographic");
		}
		
		target.append("\n  location ");
		target.appendVector(camera.getPos());
		
		if (projection.isOrthographic()) {
			
			target.append("\n  right ");
			double width = projection.getVolumeHeight()
				* projection.getAspectRatio();
			target.appendVector(camera.getRight().mult(width).invert()); //invert compensates for left-handed vs. right handed coordinates
			
			target.append("\n  up ");
			VectorXYZ up = camera.getRight().cross(camera.getViewDirection());
			target.appendVector(up.normalize().mult(projection.getVolumeHeight()));
						
			target.append("\n  look_at ");
			target.appendVector(camera.getLookAt());
						
		} else {
			
			target.append("\n  look_at  ");
			target.appendVector(camera.getLookAt());
			
		}
		
		target.append("\n}\n\n");
		
	}

	private static final void addRenderParameters(POVRayTarget target) {
		
		target.append("//chooses the season (possible values: spring, summer, autumn, winter)\n");
		target.append("#declare season=\"summer\";\n");
		target.append("//chooses the time of day (possible values: day, night)\n");
		target.append("#declare time=\"day\";\n\n\n");
				
	}
	
}
