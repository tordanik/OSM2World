package org.osm2world.core.target.obj;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

import org.osm2world.core.GlobalValues;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;

/**
 * utility class for creating an Wavefront OBJ file
 */
public final class ObjWriter {

	/** prevents instantiation */
	private ObjWriter() { }

	public static final void writeObjFile(
			File objFile, MapData mapData,
			MapProjection mapProjection,
			Camera camera, Projection projection, boolean underground)
			throws IOException {

		if (!objFile.exists()) {
			objFile.createNewFile();
		}

		File mtlFile = new File(objFile.getAbsoluteFile() + ".mtl");
		if (!mtlFile.exists()) {
			mtlFile.createNewFile();
		}

		try (
			PrintStream objStream = new PrintStream(objFile);
			PrintStream mtlStream = new PrintStream(mtlFile);
		) {

			/* write comments at the beginning of both files */

			writeObjHeader(objStream, mapProjection);

			writeMtlHeader(mtlStream);

			/* write path of mtl file to obj file */

			objStream.println("mtllib " + mtlFile.getName() + "\n");

			/* write actual file content */

			ObjTarget target = new ObjTarget(objStream, mtlStream, objFile.getParentFile());

			TargetUtil.renderWorldObjects(target, mapData, underground);

		}

	}

	public static final void writeObjFiles(
			final File objDirectory, MapData mapData,
			final MapProjection mapProjection,
			Camera camera, Projection projection,
			int primitiveThresholdPerFile)
			throws IOException {

		if (!objDirectory.exists()) {
			objDirectory.mkdir();
		}

		checkArgument(objDirectory.isDirectory());

		final File mtlFile = new File(objDirectory.getPath()
				+ File.separator + "materials.mtl");
		if (!mtlFile.exists()) {
			mtlFile.createNewFile();
		}

		final PrintStream mtlStream = new PrintStream(mtlFile);

		writeMtlHeader(mtlStream);

		/* create iterator which creates and wraps .obj files as needed */

		Iterator<ObjTarget> objIterator = new Iterator<ObjTarget>() {

			private int fileCounter = 0;
			PrintStream objStream = null;

			@Override
			public boolean hasNext() {
				return true;
			}

			@Override
			public ObjTarget next() {

				try {

					if (objStream != null) {
						objStream.close();
						fileCounter ++;
					}

					File objFile = new File(objDirectory.getPath() + File.separator
							+ "part" + format("%04d", fileCounter) + ".obj");

					if (!objFile.exists()) {
						objFile.createNewFile();
					}

					objStream = new PrintStream(objFile);

					writeObjHeader(objStream, mapProjection);

					objStream.println("mtllib " + mtlFile.getName() + "\n");

					return new ObjTarget(objStream, mtlStream, objDirectory);

				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		};

		/* write file content */

		TargetUtil.renderWorldObjects(objIterator, mapData, primitiveThresholdPerFile);

		mtlStream.close();

	}

	private static final void writeObjHeader(PrintStream objStream,
			MapProjection mapProjection) {

		objStream.println("# This file was created by OSM2World "
				+ GlobalValues.VERSION_STRING + " - "
				+ GlobalValues.OSM2WORLD_URI + "\n");
		objStream.println("# Projection information:");
		objStream.println("# Coordinate origin (0,0,0): "
				+ "lat " + mapProjection.toLat(VectorXZ.NULL_VECTOR) + ", "
				+ "lon " + mapProjection.toLon(VectorXZ.NULL_VECTOR) + ", "
				+ "ele 0");
		objStream.println("# North direction: " + new VectorXYZ(0, 0, -1));
		objStream.println("# 1 coordinate unit corresponds to roughly 1 m in reality\n");

	}

	private static final void writeMtlHeader(PrintStream mtlStream) {

		mtlStream.println("# This file was created by OSM2World "
				+ GlobalValues.VERSION_STRING + " - "
				+ GlobalValues.OSM2WORLD_URI + "\n\n");

	}

}
