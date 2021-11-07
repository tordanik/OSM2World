package org.osm2world.core.target.common;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.mesh.TriangleGeometry;
import org.osm2world.core.target.common.texcoord.PrecomputedTexCoordFunction;
import org.osm2world.core.target.common.texcoord.TexCoordFunction;

/**
 * a {@link Target} that collects everything that is being drawn as {@link Mesh}es
 */
public class MeshTarget extends AbstractTarget {

	private final List<Mesh> meshes = new ArrayList<>();

	public List<Mesh> getMeshes() {
		return meshes;
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
		meshes.add(mesh);
	}

}
