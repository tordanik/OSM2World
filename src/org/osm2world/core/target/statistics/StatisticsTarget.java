package org.osm2world.core.target.statistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.PrimitiveTarget;
import org.osm2world.core.target.common.RenderableToPrimitiveTarget;
import org.osm2world.core.target.common.Primitive.Type;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.jogl.JOGLRendererVBO;
import org.osm2world.core.world.data.WorldObject;

/**
 * a target that simply counts the primitives that are sent to it
 * to create statistics.
 */
public class StatisticsTarget extends
		PrimitiveTarget<RenderableToPrimitiveTarget> {
	
	private long[] globalCounts = new long[Stat.values().length];
	private Map<Material, long[]> countsPerMaterial = new HashMap<Material, long[]>();
	private Map<Class<?>, long[]> countsPerClass = new HashMap<Class<?>, long[]>();
	
	private WorldObject currentObject = null;
	
	private static abstract class StatImpl {
		
		public long countObject(WorldObject object) {
			return 0;
		}
		
		public long countPrimitive(Type type, Material material,
				List<VectorXYZ> vs, List<VectorXYZ> normals,
				List<List<VectorXZ>> texCoordLists) {
			return 0;
		}
		
	}
	
	public static enum Stat {
		
		OBJECT_COUNT(new StatImpl() {
			@Override public long countObject(WorldObject object) {
				return 1;
			}
		}),
		
		PRIMITIVE_COUNT(new StatImpl() {
			@Override public long countPrimitive(Type type, Material material,
					List<VectorXYZ> vs, List<VectorXYZ> normals,
					List<List<VectorXZ>> texCoordLists) {
				return 1;
			}
		}),
		
		TOTAL_TRIANGLE_COUNT(new StatImpl() {
			@Override public long countPrimitive(Type type, Material material,
					List<VectorXYZ> vs, List<VectorXYZ> normals,
					List<List<VectorXZ>> texCoordLists) {
				if (type == Type.TRIANGLES) {
					return vs.size() / 3;
				} else {
					return vs.size() - 2;
				}
			}
		}),
		
		TRIANGLES_COUNT(new StatImpl() {
			@Override public long countPrimitive(Type type, Material material,
					List<VectorXYZ> vs, List<VectorXYZ> normals,
					List<List<VectorXZ>> texCoordLists) {
				return type == Type.TRIANGLES ? 1 : 0;
			}
		}),
		
		TRIANGLE_STRIP_COUNT(new StatImpl() {
			@Override public long countPrimitive(Type type, Material material,
					List<VectorXYZ> vs, List<VectorXYZ> normals,
					List<List<VectorXZ>> texCoordLists) {
				return type == Type.TRIANGLE_STRIP ? 1 : 0;
			}
		}),
		
		TRIANGLE_FAN_COUNT(new StatImpl() {
			@Override public long countPrimitive(Type type, Material material,
					List<VectorXYZ> vs, List<VectorXYZ> normals,
					List<List<VectorXZ>> texCoordLists) {
				return type == Type.TRIANGLE_FAN ? 1 : 0;
			}
		}),
		
		CONVEX_POLYGON_COUNT(new StatImpl() {
			@Override public long countPrimitive(Type type, Material material,
					List<VectorXYZ> vs, List<VectorXYZ> normals,
					List<List<VectorXZ>> texCoordLists) {
				return type == Type.CONVEX_POLYGON ? 1 : 0;
			}
		}),
		
		VBO_VALUE_COUNT(new StatImpl() {
			@Override public long countPrimitive(Type type, Material material,
					List<VectorXYZ> vs, List<VectorXYZ> normals,
					List<List<VectorXZ>> texCoordLists) {
				
				int vertexCount;
				
				if (type == Type.TRIANGLES) {
					vertexCount = vs.size();
				} else {
					vertexCount = 3 * (vs.size() - 2);
				}
				
				return vertexCount *
					JOGLRendererVBO.getValuesPerVertex(material);
				
			}
		});
		
		private final StatImpl impl;

		private Stat(StatImpl impl) {
			this.impl = impl;
		}
		
	}
	
	@Override
	public Class<RenderableToPrimitiveTarget> getRenderableType() {
		return RenderableToPrimitiveTarget.class;
	}

	@Override
	public void render(RenderableToPrimitiveTarget renderable) {
		renderable.renderTo(this);
	}

	@Override
	public void beginObject(WorldObject object) {
		
		currentObject = object;
		
		if (currentObject != null) {
			
			Class<?> currentClass = currentObject.getClass();
			
			if (!countsPerClass.containsKey(currentClass)) {
				countsPerClass.put(currentClass,
						new long[Stat.values().length]);
			}
			
			for (Stat stat : Stat.values()) {
				
				long count = stat.impl.countObject(object);
				
				globalCounts[stat.ordinal()] += count;
							
				if (currentObject != null) {
					countsPerClass.get(currentClass)[stat.ordinal()] += count;
				}
				
			}
			
			
		}
		
		super.beginObject(object);
			
	}
	
	@Override
	protected void drawPrimitive(Type type, Material material,
			List<VectorXYZ> vs, List<VectorXYZ> normals,
			List<List<VectorXZ>> texCoordLists) {
		
		if (!countsPerMaterial.containsKey(material)) {
			countsPerMaterial.put(material, new long[Stat.values().length]);
		}
		
		for (Stat stat : Stat.values()) {
			
			long count = stat.impl.countPrimitive(
					type, material, vs, normals, texCoordLists);
			
			globalCounts[stat.ordinal()] += count;
			
			if (material != null) {
				countsPerMaterial.get(material)[stat.ordinal()] += count;
			}
			
			if (currentObject != null) {
				countsPerClass.get(currentObject.getClass())[stat.ordinal()] += count;
			}
			
		}
		
	}
	
	public void clear() {
		for (int i=0; i < globalCounts.length; ++i) {
			globalCounts[i] = 0;
		}
		countsPerMaterial.clear();
		countsPerClass.clear();
		currentObject = null;
	}
	
	public long getGlobalCount(Stat stat) {
		return globalCounts[stat.ordinal()];
	}
	
	public Set<Material> getKnownMaterials() {
		return countsPerMaterial.keySet();
	}
	
	public long getCountForMaterial(Material material, Stat stat) {
		return countsPerMaterial.get(material)[stat.ordinal()];
	}
	
	public Set<Class<?>> getKnownRenderableClasses() {
		return countsPerClass.keySet();
	}
	
	public long getCountForClass(Class<?> c, Stat stat) {
		return countsPerClass.get(c)[stat.ordinal()];
	}
	
}
