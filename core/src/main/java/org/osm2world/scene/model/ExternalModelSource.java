package org.osm2world.scene.model;

import java.io.File;

import javax.annotation.Nonnull;

/**
 * information about the source of an external model, such as {@link org.osm2world.output.gltf.GltfModel}.
 */
public interface ExternalModelSource {

	@Nonnull String toString();

	record LocalFileSource(File file) implements ExternalModelSource {
		@Override
		public @Nonnull String toString() {
			return file.getName();
		}
	}

	record External3DMRSource(long id) implements ExternalModelSource {
		@Override
		public @Nonnull String toString() {
			return "3dmr:" + id;
		}
	}

}


