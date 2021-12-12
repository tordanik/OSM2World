package org.osm2world.core.target.common;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.target.common.mesh.Geometry.combine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.osm2world.core.map_data.data.MapRelation;
import org.osm2world.core.target.common.mesh.Geometry;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.world.data.WorldObject;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/** a collection of meshes along with some metadata */
public class MeshStore {

	@FunctionalInterface
	public static interface MeshProcessingStep extends Function<MeshStore, MeshStore> {}

	public static class MeshMetadata {

		public final @Nullable MapRelation.Element mapElement;
		public final @Nullable Class<? extends WorldObject> modelClass;

		public MeshMetadata(MapRelation.Element mapElement, Class<? extends WorldObject> modelClass) {
			this.mapElement = mapElement;
			this.modelClass = modelClass;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((mapElement == null) ? 0 : mapElement.hashCode());
			result = prime * result + ((modelClass == null) ? 0 : modelClass.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MeshMetadata other = (MeshMetadata) obj;
			if (mapElement == null) {
				if (other.mapElement != null)
					return false;
			} else if (!mapElement.equals(other.mapElement))
				return false;
			if (modelClass == null) {
				if (other.modelClass != null)
					return false;
			} else if (!modelClass.equals(other.modelClass))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "{" + mapElement + ", " + modelClass.getSimpleName() + "}";
		}

	}

	public static class MeshWithMetadata {

		public final Mesh mesh;
		public final MeshMetadata metadata;

		public MeshWithMetadata(Mesh mesh, MeshMetadata metadata) {
			if (mesh == null || metadata == null) throw new NullPointerException();
			this.mesh = mesh;
			this.metadata = metadata;
		}

		public static MeshWithMetadata merge(MeshWithMetadata m1, MeshWithMetadata m2) {

			MeshMetadata metadata = (Objects.equal(m1.metadata, m2.metadata))
					? m1.metadata
					: new MeshMetadata(null, null);

			Geometry mergedGeometry = combine(asList(m1.mesh.geometry, m2.mesh.geometry));
			Mesh mergedMesh = new Mesh(mergedGeometry, m1.mesh.material);

			return new MeshWithMetadata(mergedMesh, metadata);

		}

	}

	private final List<MeshWithMetadata> meshes = new ArrayList<>();

	public void addMesh(Mesh mesh, MeshMetadata metadata) {
		addMesh(new MeshWithMetadata(mesh, metadata));
	}

	public void addMesh(MeshWithMetadata meshWithMetadata) {
		meshes.add(meshWithMetadata);
	}

	public List<Mesh> meshes() {
		return meshes.stream().map(m -> m.mesh).collect(toList());
	}

	public List<MeshWithMetadata> meshesWithMetadata() {
		return meshes;
	}

	public Multimap<MeshMetadata, Mesh> meshesByMetadata() {
		Map<Mesh, MeshWithMetadata> metadataMap = Maps.uniqueIndex(meshes, m -> m.mesh);
		return Multimaps.index(meshes(), m -> metadataMap.get(m).metadata);
	}

	public MeshStore process(List<MeshProcessingStep> processingSteps) {
		MeshStore result = this;
		for (MeshProcessingStep processingStep : processingSteps) {
			result = processingStep.apply(result);
		}
		return result;
	}

}