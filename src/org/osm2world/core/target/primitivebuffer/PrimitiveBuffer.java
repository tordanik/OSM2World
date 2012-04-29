package org.osm2world.core.target.primitivebuffer;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.Primitive.Type;
import org.osm2world.core.target.common.PrimitiveTarget;
import org.osm2world.core.target.common.RenderableToPrimitiveTarget;
import org.osm2world.core.target.common.material.Material;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Storage for low-level rendering information (vertex and primitive data)
 * that can be displayed using graphics APIs, e.g. OpenGL.
 * Higher-level information, such as object coherence, OSM attributes
 * or representations, isn't present in an OGLBuffer.
 */
public class PrimitiveBuffer extends
		PrimitiveTarget<RenderableToPrimitiveTarget> {

	@Override
	public Class<RenderableToPrimitiveTarget> getRenderableType() {
		return RenderableToPrimitiveTarget.class;
	}
	
	@Override
	public void render(RenderableToPrimitiveTarget renderable) {
		renderable.renderTo(this);
	}
	
	private ArrayList<VectorXYZ> vertexCollection = new ArrayList<VectorXYZ>();
	private Multimap<Material, Primitive> primitiveMap = HashMultimap.create();
	
	private TObjectIntMap<VectorXYZ> indexMap = new TObjectIntHashMap<VectorXYZ>();
	
	@Override
	protected void drawPrimitive(Type type, Material material,
			List<? extends VectorXYZ> vertices, VectorXYZ[] normals,
			List<List<VectorXZ>> textureCoordLists) {
		int[] indices = generateIndices(vertices);
		primitiveMap.put(material,
				new Primitive(type, indices, normals, textureCoordLists));
	}
	
	private int[] generateIndices(List<? extends VectorXYZ> newVertices) {
		int[] indices = new int[newVertices.size()];
		for (int i = 0; i < newVertices.size(); i++) {
			VectorXYZ vertex = VectorXYZ.xyz(newVertices.get(i));
			
			if (indexMap.containsKey(vertex)) {
				indices[i] = indexMap.get(vertex);
			} else {
				int nextIndex = vertexCollection.size();
				indices[i] = nextIndex;
				indexMap.put(vertex, nextIndex);
				vertexCollection.add(vertex);
			}
		}
		return indices;
	}
	
//	@Override
//	public void drawPolygon(Material material, VectorXYZ... vs) {
//		List<VectorXYZ> vsList = new ArrayList<VectorXYZ>(vs.length);
//		Collections.addAll(vsList, vs);
//		drawPrimitive(CONVEX_POLYGON, vsList, material);
//	}
	
	/**
	 * optimizes the primitives, for example by merging them into larger primitives.
	 */
	public void optimize() {
		
		mergePrimitiveGroups(Type.TRIANGLES);
				
	}

	/**
	 * merge individual primitive groups into a single
	 * large primitive group (if they have the same material)
	 */
	private void mergePrimitiveGroups(Type type) {
		
		//TODO: reactivate, seems not to solve material problem; but watch out for normals!
//		for (Material material : primitiveMap.keySet()) {
//
//			List<Primitive> primitives = new ArrayList<Primitive>();
//			List<Integer> indices = new ArrayList<Integer>();
//
//			for (Primitive primitive : primitiveMap.get(material)) {
//				if (primitive.type == type) {
//					primitives.add(primitive);
//					for (int index : primitive.indices) {
//						indices.add(index);
//					}
//				}
//			}
//
//			if (primitives.size() > 1) {
//				primitiveMap.get(material).removeAll(primitives);
//				int[] indicesArray = new int[indices.size()];
//				for (int i = 0; i < indicesArray.length; i++) {
//					indicesArray[i] = indices.get(i);
//				}
//				primitiveMap.get(material).add(new Primitive(type, indicesArray));
//			}
//
//		}
		
	}

	public VectorXYZ getVertex(int index) {
		return vertexCollection.get(index);
	}
	
	/**
	 * returns all vertices, in a list where the indices
	 * correspond to those used by {@link #getVertex(int)}
	 */
	public List<VectorXYZ> getVertices() {
		return vertexCollection;
	}
	
	/**
	 * returns all materials used in the buffer
	 */
	public Set<Material> getMaterials() {
		return primitiveMap.keySet();
	}
	
	/**
	 * returns all primitives that use a given material
	 */
	public Collection<Primitive> getPrimitives(Material material) {
		return primitiveMap.get(material);
	}
	
}
