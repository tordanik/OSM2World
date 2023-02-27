package org.osm2world.core.target.obj;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.FaceTarget;
import org.osm2world.core.target.common.material.*;
import org.osm2world.core.target.common.material.Material.Transparency;
import org.osm2world.core.target.common.material.TextureData.Wrap;
import org.osm2world.core.world.data.WorldObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.*;

import static java.awt.Color.WHITE;
import static java.lang.Math.max;
import static java.util.Collections.nCopies;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.osm2world.core.target.common.material.Material.multiplyColor;

public class ObjTarget extends FaceTarget {

	protected static final float AMBIENT_FACTOR = 0.5f;

	private final PrintStream objStream;
	private final PrintStream mtlStream;
	private final File objDirectory;
	private final File textureDirectory;

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
	 *
	 * @param objDirectory  the directory in which the obj is located.
	 * Other files (such as textures) may be written to this directory as well.
	 */
	public ObjTarget(PrintStream objStream, PrintStream mtlStream, File objDirectory, String objName) {

		this.objStream = objStream;
		this.mtlStream = mtlStream;
		this.objDirectory = objDirectory;

		this.textureDirectory = new File(objDirectory, objName + "_textures");
		textureDirectory.mkdir();

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

			String path;

			if (config != null && !config.getBoolean("alwaysCopyTextureFiles", true)
					&& texture instanceof RasterImageFileTexture) {

				path = ((ImageFileTexture)texture).getFile().getAbsolutePath();

			} else {

				String prefix = "tex-" + ((texture instanceof ImageFileTexture)
						? getBaseName(((ImageFileTexture)texture).getFile().getName()) + "-" : "");
				File textureFile = File.createTempFile(prefix, ".png", textureDirectory);
				ImageIO.write(texture.getBufferedImage(), "png", textureFile);

				// construct a relative path
				path = objDirectory.toURI().relativize(textureFile.toURI()).getPath();

			}

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
					System.err.println("Unable to export material " + name + ": " + e);
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

}

