package org.osm2world.scene.material;

/** Either a {@link Material} or a reference to one ({@link MaterialRef}) */
public interface MaterialOrRef {
	Material get();
}
