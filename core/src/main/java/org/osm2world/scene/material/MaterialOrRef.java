package org.osm2world.scene.material;

/** Either a {@link Material} or a reference to one ({@link MaterialRef}) */
public sealed interface MaterialOrRef permits Material, MaterialRef {
	Material get();
}
