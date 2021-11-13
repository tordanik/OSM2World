package org.osm2world.core.target.common;

import static java.util.stream.Collectors.toList;
import static org.osm2world.core.target.common.mesh.Geometry.combine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.mesh.TriangleGeometry;
import org.osm2world.core.target.common.texcoord.PrecomputedTexCoordFunction;
import org.osm2world.core.target.common.texcoord.TexCoordFunction;
import org.osm2world.core.world.data.WorldObject;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * a {@link Target} that collects everything that is being drawn as {@link Mesh}es
 */
public class MeshTarget extends AbstractTarget {

	protected final MultiValuedMap<WorldObject, Mesh> meshes = new ArrayListValuedHashMap<>();

	private WorldObject currentWorldObject = null;

	public List<Mesh> getMeshes() {
		List<Mesh> result = new ArrayList<>();
		for (WorldObject object : meshes.keySet()) {
			result.addAll(meshes.get(object));
		}
		return result;
	}

	@Override
	public void beginObject(WorldObject object) {
		this.currentWorldObject = object;
	}

	@Override
	public void drawTriangles(Material material, List<? extends TriangleXYZ> triangles,
			List<List<VectorXZ>> texCoordLists) {

		List<TexCoordFunction> texCoordFunctions = texCoordLists.stream()
				.map(PrecomputedTexCoordFunction::new)
				.collect(toList());

		drawMesh(new Mesh(new TriangleGeometry(new ArrayList<>(triangles), material.getInterpolation(),
				texCoordFunctions, null), material));

	}

	@Override
	public void drawMesh(Mesh mesh) {
		meshes.put(currentWorldObject, mesh);
	}

	protected static Collection<Mesh> mergeMeshes(Collection<Mesh> meshes) {

		List<Mesh> result = new ArrayList<>();

		Multimap<Material, Mesh> meshesByMaterial = Multimaps.index(meshes, m -> m.material);

		for (Material m : meshesByMaterial.keySet()) {
			result.add(new Mesh(combine(meshesByMaterial.get(m).stream().map(it -> it.geometry).collect(toList())), m));
		}

		return result;

	}

}
