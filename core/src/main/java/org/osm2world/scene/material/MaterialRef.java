package org.osm2world.scene.material;

import javax.annotation.Nonnull;

/**
 * Pointer to a named material which is guaranteed to be present regardless of the map style used.
 * (However, it may have a different appearance depending on the style, as the style overwrites the default appearance.)
 */
public class MaterialRef implements MaterialOrRef {

	public final @Nonnull String name;
	private final @Nonnull ImmutableMaterial defaultAppearance;

	public MaterialRef(@Nonnull String name, @Nonnull ImmutableMaterial defaultAppearance) {
		this.name = name;
		this.defaultAppearance = defaultAppearance;
	}

	public Material get() {

		Material material = Materials.getMaterial(name);

		if (material == null) {
			return defaultAppearance;
		} else {
			return material;
		}

	}

	@Override
	public String toString() {
		return "Ref(" + name + ")";
	}

	@Override
	public final boolean equals(Object o) {
		if (!(o instanceof MaterialRef that)) return false;
		return name.equals(that.name) && defaultAppearance.equals(that.defaultAppearance);
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + defaultAppearance.hashCode();
		return result;
	}

}
