package org.osm2world.core.target.obj;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

import org.osm2world.core.conversion.O2WConfig;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.math.geo.MapProjection;
import org.osm2world.core.target.TargetUtil;

/**
 * utility class for splitting a scene across multiple Wavefront OBJ files
 */
public final class ObjMultiFileWriter {

	/** prevents instantiation */
	private ObjMultiFileWriter() { }

	public static void writeObjFiles(
			final File objDirectory, MapData mapData, final MapProjection mapProjection,
			O2WConfig config, int primitiveThresholdPerFile)
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

		ObjTarget.writeMtlHeader(mtlStream);

		/* create iterator which creates and wraps .obj files as needed */

		Iterator<ObjTarget> objIterator = new Iterator<>() {

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

					ObjTarget.writeObjHeader(objStream, mapProjection);

					objStream.println("mtllib " + mtlFile.getName() + "\n");

					var target = new ObjTarget(objStream, mtlStream, objDirectory, "part_obj");
					target.setConfiguration(config);
					return target;

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

}
