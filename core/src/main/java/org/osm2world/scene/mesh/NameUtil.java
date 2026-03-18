package org.osm2world.scene.mesh;

import javax.annotation.Nullable;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.TextureLayer;

/**
 * Collection of methods for assigning reasonable names to {@link Material}s and {@link Mesh}es.
 */
public final class NameUtil {

	/** prevents instantiation */
	private NameUtil() {}

	/**
	 * Builds a somewhat human-readable name for a {@link Material}.
	 */
	public static @Nullable String getMaterialName(Material material, O2WConfig config) {
		return getMaterialName(material, null, config);
	}

	/**
	 * Builds a somewhat human-readable name for a {@link Material}.
	 *
	 * @param textureLayer  if not null, the name refers just to this one layer of the material
	 */
	public static @Nullable String getMaterialName(Material material, @Nullable TextureLayer textureLayer, O2WConfig config) {

		if (textureLayer == null && material.textureLayers().size() == 1) {
			textureLayer = material.textureLayers().get(0);
		}

		String name = config.mapStyle().getMaterialName(material);

		if (name == null) {
			if (textureLayer != null) {
				if (textureLayer.toString().startsWith("TextureAtlas")) {
					name = "TextureAtlas " + Integer.toHexString(material.hashCode());
				} else if (!textureLayer.toString().contains(",")) {
					name = textureLayer.toString();
				}
			}
		} else {
			if (textureLayer != null && material.textureLayers().size() > 1) {
				name += "_layer" + material.textureLayers().indexOf(textureLayer);
			}
		}

		return name;

	}

}
