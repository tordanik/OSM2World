package org.osm2world.core.target.statistics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.Primitive.Type;
import org.osm2world.core.target.common.PrimitiveTarget;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.jogl.JOGLRendererVBO;
import org.osm2world.core.world.data.WorldObject;

/**
 * a target that simply counts the primitives that are sent to it
 * to create statistics.
 */
public class StatisticsTarget extends PrimitiveTarget {

	private final long[] globalCounts = new long[Stat.values().length];
	private final Map<Material, long[]> countsPerMaterial = new HashMap<>();
	private final Map<Class<? extends WorldObject>, long[]> countsPerClass = new HashMap<>();

	private WorldObject currentObject = null;

	public enum Stat {

		OBJECT_COUNT {
			@Override public long countObject(WorldObject object) {
				return 1;
			}
		},

		PRIMITIVE_COUNT {
			@Override public long countPrimitive(Type type, Material material,
					List<VectorXYZ> vs, List<VectorXYZ> normals,
					List<List<VectorXZ>> texCoordLists) {
				return 1;
			}
		},

		TOTAL_TRIANGLE_COUNT {
			@Override public long countPrimitive(Type type, Material material,
					List<VectorXYZ> vs, List<VectorXYZ> normals,
					List<List<VectorXZ>> texCoordLists) {
				if (type == Type.TRIANGLES) {
					return vs.size() / 3;
				} else {
					return vs.size() - 2;
				}
			}
		},

		TRIANGLES_COUNT {
			@Override public long countPrimitive(Type type, Material material,
					List<VectorXYZ> vs, List<VectorXYZ> normals,
					List<List<VectorXZ>> texCoordLists) {
				return type == Type.TRIANGLES ? 1 : 0;
			}
		},

		TRIANGLE_STRIP_COUNT {
			@Override public long countPrimitive(Type type, Material material,
					List<VectorXYZ> vs, List<VectorXYZ> normals,
					List<List<VectorXZ>> texCoordLists) {
				return type == Type.TRIANGLE_STRIP ? 1 : 0;
			}
		},

		TRIANGLE_FAN_COUNT {
			@Override public long countPrimitive(Type type, Material material,
					List<VectorXYZ> vs, List<VectorXYZ> normals,
					List<List<VectorXZ>> texCoordLists) {
				return type == Type.TRIANGLE_FAN ? 1 : 0;
			}
		},

		CONVEX_POLYGON_COUNT {
			@Override public long countPrimitive(Type type, Material material,
					List<VectorXYZ> vs, List<VectorXYZ> normals,
					List<List<VectorXZ>> texCoordLists) {
				return type == Type.CONVEX_POLYGON ? 1 : 0;
			}
		},

		VBO_VALUE_COUNT {
			@Override public long countPrimitive(Type type, Material material,
					List<VectorXYZ> vs, List<VectorXYZ> normals,
					List<List<VectorXZ>> texCoordLists) {

				int vertexCount;

				if (type == Type.TRIANGLES) {
					vertexCount = vs.size();
				} else {
					vertexCount = 3 * (vs.size() - 2);
				}

				return (long) vertexCount *
					JOGLRendererVBO.getValuesPerVertex(material);

			}
		};

		public long countObject(WorldObject object) {
			return 0;
		}

		public long countPrimitive(Type type, Material material,
									List<VectorXYZ> vs, List<VectorXYZ> normals,
									List<List<VectorXZ>> texCoordLists) {
			return 0;
		}

	}

	@Override
	public void beginObject(WorldObject object) {

		currentObject = object;

		if (currentObject != null) {

			Class<? extends WorldObject> currentClass = currentObject.getClass();

			countsPerClass.putIfAbsent(currentClass, new long[Stat.values().length]);

			for (Stat stat : Stat.values()) {

				long count = stat.countObject(object);

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

		countsPerMaterial.putIfAbsent(material, new long[Stat.values().length]);

		for (Stat stat : Stat.values()) {

			long count = stat.countPrimitive(
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

	public long getGlobalCount(Stat stat) {
		return globalCounts[stat.ordinal()];
	}

	public Set<Material> getKnownMaterials() {
		return countsPerMaterial.keySet();
	}

	public long getCountForMaterial(Material material, Stat stat) {
		return countsPerMaterial.get(material)[stat.ordinal()];
	}

	public Set<Class<? extends WorldObject>> getKnownRenderableClasses() {
		return countsPerClass.keySet();
	}

	public long getCountForClass(Class<?> c, Stat stat) {
		return countsPerClass.get(c)[stat.ordinal()];
	}

}
