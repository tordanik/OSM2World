package org.osm2world.core.target.jogl;

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
 * or representations, isn't present in a PrimitiveBuffer.
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
	
	private Multimap<JOGLMaterial, Primitive> primitiveMap = HashMultimap.create();
	
	@Override
	protected void drawPrimitive(Type type, Material material,
			List<VectorXYZ> vertices, List<VectorXYZ> normals,
			List<List<VectorXZ>> texCoordLists) {
		System.err.println("PrimitiveBuffer should be used with JOGLMaterial");
	}

	void drawPrimitive(Type type, JOGLMaterial joglMaterial,
			List<VectorXYZ> vertices, List<VectorXYZ> normals,
			List<List<VectorXZ>> texCoordLists) {
		primitiveMap.put(joglMaterial,
				new Primitive(type, vertices, normals, texCoordLists));
	}
	
	/**
	 * returns all materials used in the buffer
	 */
	public Set<JOGLMaterial> getMaterials() {
		return primitiveMap.keySet();
	}
	
	/**
	 * returns all primitives that use a given material
	 */
	public Collection<Primitive> getPrimitives(JOGLMaterial material) {
		return primitiveMap.get(material);
	}
	
}
