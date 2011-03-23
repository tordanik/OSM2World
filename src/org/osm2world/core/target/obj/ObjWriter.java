package org.osm2world.core.target.obj;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.osm2world.core.GlobalValues;
import org.osm2world.core.heightmap.data.CellularTerrainElevation;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.terrain.data.Terrain;

/**
 * utility class for creating an Wavefront OBJ file
 */
public final class ObjWriter {

	/** prevents instantiation */
	private ObjWriter() { }
	
	public static final void writeObjFile(
			File objFile, MapData grid,
			CellularTerrainElevation eleData, Terrain terrain,
			MapProjection mapProjection,
			Camera camera, Projection projection)
			throws IOException {
		
		if (!objFile.exists()) {
			objFile.createNewFile();
		}
				
		File mtlFile = new File(objFile.getAbsoluteFile() + ".mtl");
		if (!mtlFile.exists()) {
			mtlFile.createNewFile();
		}
		
		PrintStream objStream = new PrintStream(objFile);
		PrintStream mtlStream = new PrintStream(mtlFile);
		
		/* write comments at the beginning of both files */
		
		objStream.println("# This file was created by OSM2World - "
				+ GlobalValues.OSM2WORLD_URI + "\n");
		objStream.println("# Projection information:");
		objStream.println("# Coordinate origin (0,0,0): "
				+ "lat " + mapProjection.calcLat(VectorXZ.NULL_VECTOR) + ", "
				+ "lon " + mapProjection.calcLon(VectorXZ.NULL_VECTOR) + ", "
				+ "ele 0");
		objStream.println("# North direction: " + new VectorXYZ(
						mapProjection.getNorthUnit().x, 0,
						- mapProjection.getNorthUnit().z));
		objStream.println("# 1 coordinate unit corresponds to roughly "
				+ "1 m in reality\n");
		
		mtlStream.println("# This file was created by OSM2World - "
				+ GlobalValues.OSM2WORLD_URI + "\n");
				
		/* write path of mtl file to obj file */
		
		objStream.println("mtllib " + mtlFile.getName() + "\n");
		
		/* write actual file content */
		
		writeObjStringToStream(objStream, mtlStream,
				grid, eleData, terrain, camera, projection);
		
		objStream.close();
		
	}

	private static final void writeObjStringToStream(
			PrintStream objStream, PrintStream matStream,
			MapData mapData,
			CellularTerrainElevation eleData, Terrain terrain,
			Camera camera, Projection projection) {
		
		ObjTarget target = new ObjTarget(objStream, matStream);
		TargetUtil.renderWorldObjects(target, mapData);
		TargetUtil.renderObject(target, terrain);
		
	}
		
}
