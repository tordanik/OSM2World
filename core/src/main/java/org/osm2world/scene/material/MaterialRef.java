package org.osm2world.scene.material;

import javax.annotation.Nonnull;

import org.osm2world.style.Style;

/**
 * Pointer to a named material which is guaranteed to be present regardless of the map style used.
 * (However, it may have a different appearance depending on the style, as the style overwrites the default appearance.)
 */
public record MaterialRef(@Nonnull String name, @Nonnull Material defaultAppearance) implements MaterialOrRef {

	@Override
	public Material get(Style style) {
		return style.resolveMaterial(this);
	}

	@Override
	public @Nonnull String toString() {
		return "Ref(" + name + ")";
	}

}
