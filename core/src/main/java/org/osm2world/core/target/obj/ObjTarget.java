package org.osm2world.core.target.obj;

import static java.awt.Color.WHITE;
import static java.lang.Math.max;
import static java.util.Collections.nCopies;
import static org.osm2world.core.target.common.material.Material.multiplyColor;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.*;

import javax.annotation.Nullable;

import org.osm2world.core.GlobalValues;
import org.osm2world.core.conversion.ConversionLog;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.geo.MapProjection;
import org.osm2world.core.math.shapes.TriangleXYZ;
import org.osm2world.core.target.common.FaceTarget;
import org.osm2world.core.target.common.ResourceOutputSettings;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Transparency;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.common.material.TextureData;
import org.osm2world.core.target.common.material.TextureData.Wrap;
import org.osm2world.core.target.common.material.TextureLayer;
import org.osm2world.core.world.data.WorldObject;

/**
 * Output models in the Wavefront OBJ format.
 * Also creates the .mtl files which go along with .obj files.
 */
public class ObjTarget extends FaceTarget {

	protected static final float AMBIENT_FACTOR = 0.5f;

	private final PrintStream objStream;
	private final PrintStream mtlStream;
	private final @Nullable File objDirectory;
	private final @Nullable File textureDirectory;

	private final Map<VectorXYZ, Integer> vertexIndexMap = new HashMap<>();
	private final Map<VectorXYZ, Integer> normalsIndexMap = new HashMap<>();
	private final Map<VectorXZ, Integer> texCoordsIndexMap = new HashMap<>();
	private final Map<Material, String> materialMap = new HashMap<>();
	private final Map<TextureData, String> textureMap = new HashMap<>();

	private Class<? extends WorldObject> currentWOGroup = null;
	private int anonymousWOCounter = 0;

	private Material currentMaterial = null;
	private int currentMaterialLayer = 0;
	private static int anonymousMaterialCounter = 0;

	// this is approximately one millimeter
	private static final double SMALL_OFFSET = 1e-3;

	/**
	 * creates an {@link ObjTarget} which writes to files on the disk.
	 * Associated files, such as the .mtl definition and any texture files will be written as well.
	 *
	 * @param mapProjection  optional information about the map projection used to create the scene,
	 * can be used to add information to the output file
	 */
	public ObjTarget(File objFile, @Nullable MapProjection mapProjection) throws IOException {

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

		writeObjHeader(objStream, mapProjection);

		writeMtlHeader(mtlStream);

		/* write path of mtl file to obj file */

		objStream.println("mtllib " + mtlFile.getName() + "\n");

		/* set up the streams to write the rest of the content to */

		this.objStream = objStream;
		this.mtlStream = mtlStream;
		this.objDirectory = objFile.getAbsoluteFile().getParentFile();
		this.textureDirectory = getTextureDirectory(objDirectory, objFile.getName());

	}

	/**
	 * creates an {@link ObjTarget} which writes to streams, not necessarily {@link File}s.
	 *
	 * @param objDirectory  the directory in which the obj is located.
	 * Other files (such as textures) may be written to this directory as well.
	 * Can be null, but that prevents texture files from being written.
	 * @param objName  optional name of the OBJ, used to name associated files such as textures
	 */
	public ObjTarget(PrintStream objStream, PrintStream mtlStream,
			@Nullable File objDirectory, @Nullable String objName) {

		this.objStream = objStream;
		this.mtlStream = mtlStream;
		this.objDirectory = objDirectory;

		this.textureDirectory = objDirectory == null || objName == null
			? null : getTextureDirectory(objDirectory, objName);

	}

	private static File getTextureDirectory(File objDirectory, String objName) {
		return new File(objDirectory, objName + "_textures");
	}

	@Override
	public boolean reconstructFaces() {
		return config != null && config.getBoolean("reconstructFaces", false);
	}

	@Override
	public void beginObject(WorldObject object) {

		if (object == null) {

			currentWOGroup = null;
			objStream.println("g null");
			objStream.println("o null");

		} else {

			/* maybe start a group depending on the object's class */

			if (!object.getClass().equals(currentWOGroup)) {
				currentWOGroup = object.getClass();
				objStream.println("g " + currentWOGroup.getSimpleName());
			}

			/* start an object with the object's class
			 * and the underlying OSM element's name/ref tags */

			TagSet tags = object.getPrimaryMapElement().getTags();

			if (tags.containsKey("name")) {
				objStream.println("o " + object.getClass().getSimpleName() + " " + tags.getValue("name"));
			} else if (tags.containsKey("ref")) {
				objStream.println("o " + object.getClass().getSimpleName() + " " + tags.getValue("ref"));
			} else {
				objStream.println("o " + object.getClass().getSimpleName() + anonymousWOCounter ++);
			}

		}

	}

	@Override
	public void drawFace(Material material, List<VectorXYZ> vs,
			List<VectorXYZ> normals, List<List<VectorXZ>> texCoordLists) {

		int[] normalIndices = null;
		if (normals != null) {
			normalIndices = normalsToIndices(normals);
		}

		VectorXYZ faceNormal = new TriangleXYZ(vs.get(0), vs.get(1), vs.get(2)).getNormal();

		for (int layer = 0; layer < max(1, material.getNumTextureLayers()); layer++) {

			useMaterial(material, layer);

			int[] texCoordIndices = null;
			if (texCoordLists != null && !texCoordLists.isEmpty()) {
				texCoordIndices = texCoordsToIndices(texCoordLists.get(layer));
			}

			writeFace(verticesToIndices((layer == 0)? vs : offsetVertices(vs, nCopies(vs.size(), faceNormal), layer * SMALL_OFFSET)),
					normalIndices, texCoordIndices);
		}
	}

	private void useMaterial(Material material, int layer) {
		if (!material.equals(currentMaterial) || (layer != currentMaterialLayer)) {

			String name = materialMap.get(material);
			if (name == null) {
				name = Materials.getUniqueName(material);
				if (name == null) {
					name = "MAT_" + anonymousMaterialCounter;
					anonymousMaterialCounter += 1;
				}
				materialMap.put(material, name);
				writeMaterial(material, name);
			}

			objStream.println("usemtl " + name + "_" + layer);

			currentMaterial = material;
			currentMaterialLayer = layer;
		}
	}

	private List<? extends VectorXYZ> offsetVertices(List<? extends VectorXYZ> vs, List<VectorXYZ> directions, double offset) {

		List<VectorXYZ> result = new ArrayList<VectorXYZ>(vs.size());

		for (int i = 0; i < vs.size(); i++) {
			result.add(vs.get(i).add(directions.get(i).mult(offset)));
		}

		return result;
	}

	private int[] verticesToIndices(List<? extends VectorXYZ> vs) {
		return vectorsToIndices(vertexIndexMap, "v ", vs);
	}

	private int[] normalsToIndices(List<? extends VectorXYZ> normals) {
		return vectorsToIndices(normalsIndexMap, "vn ", normals);
	}

	private int[] texCoordsToIndices(List<VectorXZ> texCoords) {
		return vectorsToIndices(texCoordsIndexMap, "vt ", texCoords);
	}

	private <V> int[] vectorsToIndices(Map<V, Integer> indexMap,
			String objLineStart, List<? extends V> vectors) {

		int[] indices = new int[vectors.size()];

		for (int i=0; i<vectors.size(); i++) {
			final V v = vectors.get(i);
			Integer index = indexMap.get(v);
			if (index == null) {
				index = indexMap.size();
				objStream.println(objLineStart + " " + formatVector(v));
				indexMap.put(v, index);
			}
			indices[i] = index;
		}

		return indices;

	}

	private String formatVector(Object v) {

		if (v instanceof VectorXYZ) {
			VectorXYZ vXYZ = (VectorXYZ)v;
			return vXYZ.x + " " + vXYZ.y + " " + (-vXYZ.z);
		} else {
			VectorXZ vXZ = (VectorXZ)v;
			return vXZ.x + " " + vXZ.z;
		}

	}

	/** returns the texture's path as a String; creates a file in the output directory if necessary */
	private String textureToPath(TextureData texture) throws IOException {

		if (!textureMap.containsKey(texture)) {

			ResourceOutputSettings resourceOutputSettings = ResourceOutputSettings.fromConfig(config, textureDirectory.toURI(), false);

			String path = switch (resourceOutputSettings.modeForTexture(texture)) {
				case REFERENCE -> resourceOutputSettings.buildTextureReference(texture);
				case STORE_SEPARATELY_AND_REFERENCE -> resourceOutputSettings.storeTexture(texture, objDirectory.toURI());
				case EMBED -> throw new UnsupportedOperationException("unsupported output mode");
			};

			textureMap.put(texture, path);

		}

		return textureMap.get(texture);

	}

	private void writeFace(int[] vertexIndices, int[] normalIndices,
			int[] texCoordIndices) {

		assert normalIndices == null
				|| vertexIndices.length == normalIndices.length;

		//Don't add faces with duplicate vertices.
		HashSet<Integer> set = new HashSet<Integer>();
		for(int element : vertexIndices)
		{
			if(!set.add(element))
			{
				return;
			}
		}

		objStream.print("f");

		for (int i = 0; i < vertexIndices.length; i++) {

			objStream.print(" " + (vertexIndices[i]+1));

			if (texCoordIndices != null && normalIndices == null) {
				objStream.print("/" + (texCoordIndices[i]+1));
			} else if (texCoordIndices == null && normalIndices != null) {
				objStream.print("//" + (normalIndices[i]+1));
			} else if (texCoordIndices != null && normalIndices != null) {
				objStream.print("/" + (texCoordIndices[i]+1)
						+ "/" + (normalIndices[i]+1));
			}

		}

		objStream.println();
	}

	private void writeMaterial(Material material, String name) {

		for (int i = 0; i < max(1, material.getNumTextureLayers()); i++) {

			TextureLayer textureLayer = null;
			if (material.getNumTextureLayers() > 0) {
				textureLayer = material.getTextureLayers().get(i);
			}

			mtlStream.println("newmtl " + name + "_" + i);
			mtlStream.println("Ns 92.156863");

			if (textureLayer == null || textureLayer.colorable) {
				writeColorLine("Ka", multiplyColor(material.getColor(), AMBIENT_FACTOR));
				writeColorLine("Kd", multiplyColor(material.getColor(), 1 - AMBIENT_FACTOR));
			} else {
				writeColorLine("Ka", multiplyColor(WHITE, AMBIENT_FACTOR));
				writeColorLine("Kd", multiplyColor(WHITE, 1 - AMBIENT_FACTOR));
			}

			float specularFactor = 0f;
			mtlStream.println(String.format(Locale.US ,"Ks %f %f %f", specularFactor, specularFactor, specularFactor));
			mtlStream.println(String.format(Locale.US ,"Ke %f %f %f", 0f, 0f, 0f));

			if (textureLayer != null) {
				try {

					String clamp = (textureLayer.baseColorTexture.wrap == Wrap.REPEAT) ? "" : "-clamp on ";

					mtlStream.println("map_Ka " + clamp + textureToPath(textureLayer.baseColorTexture));
					mtlStream.println("map_Kd " + clamp + textureToPath(textureLayer.baseColorTexture));

					if (material.getTransparency() != Transparency.FALSE) {
						mtlStream.println("map_d " + clamp + textureToPath(textureLayer.baseColorTexture));
					}

				} catch (IOException e) {
					ConversionLog.error("Unable to export material " + name + ": ", e);
				}
			}

			int shininess = 1;
			mtlStream.println(String.format("Ni %d", shininess));
			mtlStream.println("illum 2");

			mtlStream.println();
		}
	}

	private void writeColorLine(String lineStart, Color color) {

		mtlStream.println(lineStart
				+ " " + color.getRed() / 255f
				+ " " + color.getGreen() / 255f
				+ " " + color.getBlue() / 255f);

	}

	static void writeObjHeader(PrintStream objStream,
			@Nullable MapProjection mapProjection) {

		objStream.println("# This file was created by OSM2World "
				+ GlobalValues.VERSION_STRING + " - "
				+ GlobalValues.OSM2WORLD_URI + "\n");
		objStream.println("# Projection information:");
		if (mapProjection != null) {
			objStream.println("# Coordinate origin (0,0,0): "
					+ "lat " + mapProjection.getOrigin().lat + ", "
					+ "lon " + mapProjection.getOrigin().lon + ", "
					+ "ele 0");
		}
		objStream.println("# North direction: " + new VectorXYZ(0, 0, -1));
		objStream.println("# 1 coordinate unit corresponds to roughly 1 m in reality\n");

	}

	static void writeMtlHeader(PrintStream mtlStream) {

		mtlStream.println("# This file was created by OSM2World "
				+ GlobalValues.VERSION_STRING + " - "
				+ GlobalValues.OSM2WORLD_URI + "\n\n");

	}

}

