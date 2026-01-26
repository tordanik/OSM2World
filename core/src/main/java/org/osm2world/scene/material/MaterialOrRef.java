package org.osm2world.scene.material;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.style.Style;

/** Either a {@link Material} or a reference to one ({@link MaterialRef}) */
public sealed interface MaterialOrRef permits Material, MaterialRef {
	Material get(Style mapStyle);
	default Material get(O2WConfig config) { return get(config.mapStyle()); }
}
