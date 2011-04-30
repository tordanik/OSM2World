package org.osm2world.core.target.obj;

import java.awt.Color;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXYZWithNormals;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.osm.data.OSMElement;
import org.osm2world.core.target.common.AbstractTarget;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.data.WorldObject;

public class ObjTarget extends AbstractTarget<RenderableToObj> {

	private final PrintStream objStream;
	private final PrintStream mtlStream;
	
	private final Map<VectorXYZ, Integer> vertexIndexMap = new HashMap<VectorXYZ, Integer>();
	private final Map<VectorXYZ, Integer> normalsIndexMap = new HashMap<VectorXYZ, Integer>();
	private final Map<Material, String> materialMap = new HashMap<Material, String>();
	
	private Class<? extends WorldObject> currentWOGroup = null;
	private int anonymousWOCounter = 0;
	
	private Material currentMaterial = null;
	private int anonymousMaterialCounter = 0;
	
	public ObjTarget(PrintStream objStream, PrintStream mtlStream) {
		
		this.objStream = objStream;
		this.mtlStream = mtlStream;
				
	}
	
	@Override
	public Class<RenderableToObj> getRenderableType() {
		return RenderableToObj.class;
	}
	
	@Override
	public void render(RenderableToObj renderable) {
		renderable.renderTo(this);
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
			
			MapElement element = object.getPrimaryMapElement();
			OSMElement osmElement;
			if (element instanceof MapNode) {
				osmElement = ((MapNode) element).getOsmNode();
			} else if (element instanceof MapWaySegment) {
				osmElement = ((MapWaySegment) element).getOsmWay();
			} else if (element instanceof MapArea) {
				osmElement = ((MapArea) element).getOsmObject();
			} else {
				osmElement = null;
			}
			
			if (osmElement != null && osmElement.tags.containsKey("name")) {
				objStream.println("o " + object.getClass().getSimpleName() + " " + osmElement.tags.getValue("name"));
			} else if (osmElement != null && osmElement.tags.containsKey("ref")) {
				objStream.println("o " + object.getClass().getSimpleName() + " " + osmElement.tags.getValue("ref"));
			} else {
				objStream.println("o " + object.getClass().getSimpleName() + anonymousWOCounter ++);
			}
			
		}
		
	}
	
	@Override
	public void drawTriangles(Material material,
			Collection<? extends TriangleXYZ> triangles) {
		
		useMaterial(material);
		
		for (TriangleXYZ triangle : triangles) {
			writeFace(verticesToIndices(triangle.getVertices()), null);
		}
		
	}

	@Override
	public void drawTrianglesWithNormals(Material material,
			Collection<? extends TriangleXYZWithNormals> triangles) {
		
		useMaterial(material);
		
		for (TriangleXYZWithNormals triangle : triangles) {
			writeFace(verticesToIndices(triangle.getVertices()),
					normalsToIndices(triangle.getNormals()));
		}
		
	}
	
	@Override
	public void drawPolygon(Material material, VectorXYZ... vs) {
		useMaterial(material);
		writeFace(verticesToIndices(Arrays.asList(vs)), null);
	}

	private void useMaterial(Material material) {
		if (!material.equals(currentMaterial)) {
			
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
			
			objStream.println("usemtl " + name);
			
		}
	}
	
	private int[] verticesToIndices(List<? extends VectorXYZ> vs) {
		return vectorsToIndices(vertexIndexMap, "v ", vs);
	}

	private int[] normalsToIndices(List<? extends VectorXYZ> normals) {
		return vectorsToIndices(normalsIndexMap, "vn ", normals);
	}
	
	private int[] vectorsToIndices(Map<VectorXYZ, Integer> indexMap,
			String objLineStart, List<? extends VectorXYZ> vectors) {
		
		int[] indices = new int[vectors.size()];
		
		for (int i=0; i<vectors.size(); i++) {
			final VectorXYZ v = vectors.get(i);
			Integer index = indexMap.get(v);
			if (index == null) {
				index = indexMap.size();
				objStream.println(objLineStart + " " + v.x + " " + v.y + " " + (-v.z));
				indexMap.put(v, index);
			}
			indices[i] = index;
		}
		
		return indices;
		
	}

	private void writeFace(int[] vertexIndices, int[] normalIndices) {

		assert normalIndices == null
				|| vertexIndices.length == normalIndices.length;

		objStream.print("f");

		for (int i = 0; i < vertexIndices.length; i++) {

			objStream.print(" " + (vertexIndices[i]+1));

			if (normalIndices != null) {
				objStream.print("//" + normalIndices[i]);
			}

		}

		objStream.println();
	}
	
	private void writeMaterial(Material material, String name) {
		
		mtlStream.println("newmtl " + name);
		writeColorLine("Ka", material.ambientColor());
		writeColorLine("Kd", material.diffuseColor());
		//Ks
		//Ns
		mtlStream.println();
		
	}
	
	private void writeColorLine(String lineStart, Color color) {
		
		mtlStream.println(lineStart
				+ " " + color.getRed() / 255f
				+ " " + color.getGreen() / 255f
				+ " " + color.getBlue() / 255f);
		
	}

}
