package org.osm2world.target.obj;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.target.common.FaceTarget;
import org.osm2world.target.common.material.Material;
import org.osm2world.world.data.WorldObject;

/**
 * specialized alternative to {@link ObjTarget} which can split a large scene across multiple obj files.
 * It starts a new file whenever a threshold is reached.
 */
public class ObjMultiFileTarget extends FaceTarget {

	private final File objDirectory;
	private final @Nullable MapProjection mapProjection;
	private final int primitiveThresholdPerFile;

	private final File mtlFile;
	private final PrintWriter mtlWriter;

	private ObjTarget currentTarget;
	private int primitivesInCurrentTarget = 0;

	/* iterator which creates and wraps .obj files as needed */
	private final Iterator<ObjTarget> objTargetIterator = new Iterator<>() {

		private int fileCounter = 0;
		private PrintWriter objWriter = null;

		@Override
		public boolean hasNext() {
			return true;
		}

		@Override
		public ObjTarget next() {

			try {

				if (objWriter != null) {
					objWriter.close();
					fileCounter ++;
				}

				File objFile = new File(objDirectory.getPath() + File.separator
						+ "part" + format("%04d", fileCounter) + ".obj");

				if (!objFile.exists()) {
					objFile.createNewFile();
				}

				objWriter = new PrintWriter(objFile);

				ObjTarget.writeObjHeader(objWriter, mapProjection);

				objWriter.println("mtllib " + mtlFile.getName() + "\n");

				var target = new ObjTarget(objWriter, mtlWriter, objDirectory, "part_obj");
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

	/**
	 * @param objDirectory  directory where the obj files and others (such as mtl and textures) will be created
	 * @param mapProjection  optional information about the map projection used to create the scene,
	 * can be used to add information to the output file
	 * @param primitiveThresholdPerFile  the number of primitives after which a new file will be started
	 */
	public ObjMultiFileTarget(File objDirectory, @Nullable MapProjection mapProjection, int primitiveThresholdPerFile) throws IOException {

		this.objDirectory = objDirectory;
		this.mapProjection = mapProjection;
		this.primitiveThresholdPerFile = primitiveThresholdPerFile;

		if (!objDirectory.exists()) {
			objDirectory.mkdir();
		}

		checkArgument(objDirectory.isDirectory());

		mtlFile = new File(objDirectory.getPath() + File.separator + "materials.mtl");
		if (!mtlFile.exists()) {
			mtlFile.createNewFile();
		}

		mtlWriter = new PrintWriter(mtlFile);

		ObjTarget.writeMtlHeader(mtlWriter);

		currentTarget = objTargetIterator.next();

	}

	@Override
	public boolean reconstructFaces() {
		return config.getBoolean("reconstructFaces", false);
	}

	@Override
	public void beginObject(WorldObject object) {
		currentTarget.beginObject(object);
		if (primitivesInCurrentTarget >= primitiveThresholdPerFile) {
			currentTarget.finish();
			currentTarget = objTargetIterator.next();
			primitivesInCurrentTarget = 0;
		}
	}

	@Override
	public void drawFace(Material material, List<VectorXYZ> vs, List<VectorXYZ> normals, List<List<VectorXZ>> texCoordLists) {
		primitivesInCurrentTarget += vs.size() - 2;
		currentTarget.drawFace(material, vs, normals, texCoordLists);
	}

	@Override
	public void finish() {
		mtlWriter.close();
		currentTarget.finish();
	}

}
