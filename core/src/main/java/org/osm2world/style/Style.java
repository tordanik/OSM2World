package org.osm2world.style;

import static java.util.Objects.requireNonNullElse;
import static org.osm2world.scene.material.Materials.GLASS_TRANSPARENT;

import java.util.Collection;

import javax.annotation.Nullable;

import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.MaterialOrRef;
import org.osm2world.scene.material.MaterialRef;

/**
 * A map style for OSM2World. It defines materials, models, and rules which control the visual appearance of the scene.
 */
public interface Style {

	/** Returns all materials defined in this style */
	Collection<Material> getMaterials();

	/**
	 * Returns a material defined in this style based on its name.
	 *
	 * @param name  case-insensitive name of the material
	 */
	@Nullable Material resolveMaterial(@Nullable String name);

	/** Variant of {@link #resolveMaterial(String)} with a default value */
	default Material resolveMaterial(@Nullable String name, MaterialOrRef defaultValue) {
		Material result = resolveMaterial(name);
		return result == null ? defaultValue.get() : result;
	}

	default Material resolveMaterial(MaterialOrRef materialOrRef) {
		if (materialOrRef instanceof MaterialRef materialRef) {
			Material material = resolveMaterial(materialRef.name());
			return material != null ? material : materialRef.defaultAppearance();
		} else {
			return (Material) materialOrRef;
		}
	}

	/**
	 * Returns a human-readable name for a material defined in this style,
	 * null for all other materials.
	 * To handle equal materials known under different names,
	 * it attempts to look for object identity before object equality.
	 */
	String getUniqueName(MaterialOrRef material);

	/**
	 * Returns the transparent variant of a material, if available.
	 * For example, there may be an equivalent of GLASS_WALL that has partially transparent glass panes
	 * and should be used if the space behind the wall is also modeled in 3D.
	 *
	 * @return the transparent variant of the material, or null if none is available
	 */
	default @Nullable Material getTransparentVariant(MaterialOrRef material) {
		return switch (requireNonNullElse(getUniqueName(material), "")) {
			case "GLASS" -> GLASS_TRANSPARENT.get();
			case "GLASS_WALL" -> resolveMaterial("GLASS_WALL_TRANSPARENT", null);
			case "GLASS_ROOF" -> resolveMaterial("GLASS_ROOF_TRANSPARENT", null);
			default -> null;
		};
	}

}
