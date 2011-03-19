package org.osm2world.core.target.primitivebuffer;

import static org.osm2world.core.util.FaultTolerantIterationUtil.iterate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.Material;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.common.Primitive;
import org.osm2world.core.target.common.PrimitiveTarget;
import org.osm2world.core.target.common.Primitive.Type;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.util.FaultTolerantIterationUtil.Operation;

/**
 * Storage for low-level rendering information (vertex and primitive data)
 * that can be displayed using graphics APIs, e.g. OpenGL.
 * Higher-level information, such as object coherence, OSM attributes
 * or representations, isn't present in an OGLBuffer.
 */
public class PrimitiveBuffer extends PrimitiveTarget {

	private ArrayList<VectorXYZ> vertexCollection = new ArrayList<VectorXYZ>();
	private Map<Material, List<Primitive>> primitiveMap = new HashMap<Material, List<Primitive>>();
	
	private Map<VectorXYZ, Integer> indexMap = new HashMap<VectorXYZ, Integer>();
	
	@Override
	protected void drawPrimitive(Type type, Material material,
			List<? extends VectorXYZ> vertices, VectorXYZ[] normals) {
		int[] indices = generateIndices(vertices);
		addPrimitive(material, new Primitive(type, indices, normals));	
	}
	
	private int[] generateIndices(List<? extends VectorXYZ> newVertices) {
		int[] indices = new int[newVertices.size()];
		for (int i = 0; i < newVertices.size(); i++) {
			VectorXYZ vertex = VectorXYZ.xyz(newVertices.get(i));
			Integer existingIndex = indexMap.get(vertex);
			if (existingIndex != null) {
				indices[i] = existingIndex;
			} else {
				int nextIndex = vertexCollection.size();
				indices[i] = nextIndex;
				indexMap.put(vertex, nextIndex);
				vertexCollection.add(vertex);
			}
		}
		return indices;
	}
	
	private void addPrimitive(Material material, Primitive primitive) {
		
		List<Primitive> primitiveList = primitiveMap.get(material);
		
		if (primitiveList == null) {
			primitiveList = new ArrayList<Primitive>();
			primitiveMap.put(material, primitiveList);
		}
		
		primitiveList.add(primitive);
		
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
	
	public void addWorldObjects(final MapData mapData, final Camera camera) {
		
		iterate(mapData.getWorldObjects(Renderable.class), new Operation<Renderable>() {
			@Override public void perform(Renderable input) {
				
				if (input instanceof RenderableToPrimitiveBuffer) {
					((RenderableToPrimitiveBuffer) input).renderTo(PrimitiveBuffer.this);
				} else if (input instanceof RenderableToAllTargets) {
					((RenderableToAllTargets) input).renderTo(PrimitiveBuffer.this);
				}
				
			}
		});
		
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
