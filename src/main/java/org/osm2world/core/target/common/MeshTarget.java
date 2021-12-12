package org.osm2world.core.target.common;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.MeshStore.MeshMetadata;
import org.osm2world.core.target.common.MeshStore.MeshProcessingStep;
import org.osm2world.core.target.common.MeshStore.MeshWithMetadata;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.mesh.TriangleGeometry;
import org.osm2world.core.target.common.texcoord.PrecomputedTexCoordFunction;
import org.osm2world.core.target.common.texcoord.TexCoordFunction;
import org.osm2world.core.world.data.WorldObject;

/**
 * a {@link Target} that collects everything that is being drawn as {@link Mesh}es
 */
public class MeshTarget extends AbstractTarget {

	protected final MeshStore meshStore = new MeshStore();

	private WorldObject currentWorldObject = null;

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

		MeshMetadata metadata = (currentWorldObject != null)
				? new MeshMetadata(currentWorldObject.getPrimaryMapElement().getElementWithId(),
						currentWorldObject.getClass())
				: new MeshMetadata(null, null);

		meshStore.addMesh(mesh, metadata);

	}

	public List<Mesh> getMeshes() {
		return meshStore.meshes();
	}

	public static class MergeMeshes implements MeshProcessingStep {

		/** options that alter the behavior away from the default */
		public enum MergeOption {

			/**
			 * whether meshes should be merged across distinct OSM elements.
			 * If this is enabled, the result will not have any {@link MeshMetadata}.
			 */
			MERGE_ELEMENTS,

			/** whether meshes should be kept separate if they have different {@link Material#getInterpolation()} */
			SEPARATE_NORMAL_MODES,

			/** whether meshes should be kept separate if they have different {@link Material#getColor()} */
			SINGLE_COLOR_MESHES

			// TODO: add PRESERVE_GEOMETRY_TYPES option

		}

		private final Set<MergeOption> options;

		public MergeMeshes(Set<MergeOption> options) {
			this.options = options;
		}

		/** checks if two meshes should be merged according to the MergeOptions */
		public boolean shouldBeMerged(MeshWithMetadata m1, MeshWithMetadata m2) {

			if (!options.contains(MergeOption.MERGE_ELEMENTS)
					&& !Objects.equals(m1.metadata, m2.metadata)) {
				return false;
			}

			return m1.mesh.material.equals(m2.mesh.material,
					!options.contains(MergeOption.SEPARATE_NORMAL_MODES),
					!options.contains(MergeOption.SINGLE_COLOR_MESHES));

		}

		@Override
		public MeshStore apply(MeshStore meshStore) {

			/* merge meshes */

			List<MeshWithMetadata> result = new ArrayList<>();

			for (MeshWithMetadata mesh : meshStore.meshesWithMetadata()) {

				boolean merged = false;

				for (int i = 0; i < result.size(); i++) {

					MeshWithMetadata existingMesh = result.get(i);

					if (shouldBeMerged(mesh, existingMesh)) {
						result.set(i, MeshWithMetadata.merge(mesh, existingMesh));
						merged = true;
						break;
					}

				}

				if (!merged) {
					result.add(mesh);
				}

			}

			/* build and return a MeshStore with the results */

			MeshStore resultingStore = new MeshStore();
			result.forEach(resultingStore::addMesh);
			return resultingStore;

		}

	}


	// TODO: implement additional processing steps
	// * EmulateTextureLayers
	// * EmulateDoubleSidedMaterials
	// * GenerateTextureAtlas
	// * ClipToBounds(bounds)
	// * FilterLod(LevelOfDetail targetLod)


}