package org.osm2world.core.world.modules.common;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.osm2world.core.target.Material;
import org.osm2world.core.target.Material.Lighting;
import org.osm2world.core.world.creation.WorldModule;

/**
 * this class defines materials that can be used by all {@link WorldModule}s
 */
public final class Materials {

	/** prevents instantiation */
	private Materials() {}

	/** material for "empty" ground */
	public static final Material EMPTY_GROUND =
		new Material(Lighting.SMOOTH, Color.GREEN);

	public static final Material WATER =
		new Material(Lighting.FLAT, Color.BLUE);

	public static final Material ASPHALT =
		new Material(Lighting.FLAT, new Color(0.3f, 0.3f, 0.3f));
	public static final Material EARTH =
		new Material(Lighting.FLAT, new Color( 0.3f, 0, 0));
	public static final Material GRASS =
		new Material(Lighting.FLAT, new Color(0.0f, 0.8f, 0.0f));
	public static final Material GRAVEL =
		new Material(Lighting.FLAT, new Color(0.4f, 0.4f, 0.4f));
	public static final Material SAND =
		new Material(Lighting.FLAT, new Color(241, 233, 80));
	public static final Material WOOD =
		new Material(Lighting.FLAT, new Color(0.3f, 0.2f, 0.2f));
	public static final Material TARTAN =
		new Material(Lighting.FLAT, new Color(206, 109, 90));

	private static final Map<String, Material> surfaceMaterialMap = new HashMap<String, Material>();
	private static final Map<Material, String> fieldNameMap = new HashMap<Material, String>();

	static {
		
		surfaceMaterialMap.put("asphalt", ASPHALT);
		surfaceMaterialMap.put("cobblestone", ASPHALT);
		surfaceMaterialMap.put("compacted", GRAVEL);
		surfaceMaterialMap.put("concrete", ASPHALT);
		surfaceMaterialMap.put("grass", GRASS);
		surfaceMaterialMap.put("gravel", GRAVEL);
		surfaceMaterialMap.put("grass_paver", ASPHALT);
		surfaceMaterialMap.put("ground", EARTH);
		surfaceMaterialMap.put("paved", ASPHALT);
		surfaceMaterialMap.put("paving_stones", ASPHALT);
		surfaceMaterialMap.put("pebblestone", ASPHALT);
		surfaceMaterialMap.put("sand", SAND);
		surfaceMaterialMap.put("tartan", TARTAN);
		surfaceMaterialMap.put("unpaved", EARTH);
		surfaceMaterialMap.put("wood", WOOD);

		try {
			for (Field field : Materials.class.getFields()) {
				if (field.getType().equals(Material.class)) {
					fieldNameMap.put((Material)field.get(null), field.getName());
				}
			}
		} catch (Exception e) {
			throw new Error(e);
		}
		
	}

	/** returns all materials defined here */
	public static final Collection<Material> getMaterials() {
		return fieldNameMap.keySet();
	}
	
	/** returns a material for a surface value; null if none is found */
	public static final Material getMaterial(String value) {
		return getMaterial(value, null);
	}
	
	/** same as {@link #getMaterial(String)}, but with fallback value */
	public static final Material getMaterial(String value, Material fallback) {
		Material material = surfaceMaterialMap.get(value);
		if (material != null) {
			return material;
		} else {
			return fallback;
		}
	}
	
	/**
	 * returns a human-readable, unique name for a material defined
	 * within this class, null for all other materials.
	 */
	public static final String getUniqueName(Material material) {
		return fieldNameMap.get(material);
	}
	
}
