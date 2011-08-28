package org.osm2world.core.target.common.material;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.target.common.material.Material.Lighting;
import org.osm2world.core.world.creation.WorldModule;

/**
 * this class defines materials that can be used by all {@link WorldModule}s
 */
public final class Materials {
	
	/** prevents instantiation */
	private Materials() {}
	
	/** material for "empty" ground */
	public static final ConfMaterial TERRAIN_DEFAULT =
		new ConfMaterial(Lighting.SMOOTH, Color.GREEN);
	
	public static final ConfMaterial WATER =
		new ConfMaterial(Lighting.FLAT, Color.BLUE);
	
	public static final ConfMaterial ASPHALT =
		new ConfMaterial(Lighting.FLAT, new Color(0.3f, 0.3f, 0.3f));
	public static final ConfMaterial CONCRETE =
		new ConfMaterial(Lighting.FLAT, new Color(0.4f, 0.4f, 0.4f));
	public static final ConfMaterial EARTH =
		new ConfMaterial(Lighting.FLAT, new Color(0.3f, 0, 0));
	public static final ConfMaterial GRASS =
		new ConfMaterial(Lighting.FLAT, new Color(0.0f, 0.8f, 0.0f));
	public static final ConfMaterial GRAVEL =
		new ConfMaterial(Lighting.FLAT, new Color(0.4f, 0.4f, 0.4f));
	public static final ConfMaterial SAND =
		new ConfMaterial(Lighting.FLAT, new Color(241, 233, 80));
	public static final ConfMaterial STEEL =
		new ConfMaterial(Lighting.FLAT, new Color(200, 200, 200));
	public static final ConfMaterial WOOD =
		new ConfMaterial(Lighting.FLAT, new Color(0.3f, 0.2f, 0.2f));
	public static final ConfMaterial TARTAN =
		new ConfMaterial(Lighting.FLAT, new Color(206, 109, 90));
	
	public static final ConfMaterial ROAD_MARKING =
		new ConfMaterial(Lighting.FLAT, new Color(1f, 1f, 1f));
	public static final ConfMaterial STEPS_DEFAULT =
		new ConfMaterial(Lighting.FLAT, Color.DARK_GRAY);
	public static final ConfMaterial HANDRAIL_DEFAULT =
		new ConfMaterial(Lighting.FLAT, Color.LIGHT_GRAY);
		
	public static final ConfMaterial RAIL_DEFAULT =
		new ConfMaterial(Lighting.FLAT, Color.LIGHT_GRAY);
	public static final ConfMaterial RAIL_SLEEPER_DEFAULT =
		new ConfMaterial(Lighting.FLAT, new Color(0.3f, 0.2f, 0.2f));
	public static final ConfMaterial RAIL_BALLAST_DEFAULT =
		new ConfMaterial(Lighting.FLAT, Color.DARK_GRAY);
	
	public static final ConfMaterial BUILDING_DEFAULT =
		new ConfMaterial(Lighting.FLAT, new Color(1f, 0.9f, 0.55f));
	public static final ConfMaterial ROOF_DEFAULT =
		new ConfMaterial(Lighting.FLAT, new Color(0.8f, 0, 0));
	public static final ConfMaterial ENTRANCE_DEFAULT =
		new ConfMaterial(Lighting.FLAT, new Color(0.2f, 0, 0));
	
	public static final ConfMaterial WALL_DEFAULT =
		new ConfMaterial(Lighting.FLAT, Color.GRAY);
	
	public static final ConfMaterial HEDGE =
		new ConfMaterial(Lighting.FLAT, new Color(0,0.5f,0));
	
	public static final ConfMaterial FENCE_DEFAULT =
		new ConfMaterial(Lighting.FLAT, new Color(0.3f, 0.2f, 0.2f));
	public static final ConfMaterial SPLIT_RAIL_FENCE =
		new ConfMaterial(Lighting.FLAT, new Color(0.3f, 0.2f, 0.2f));
	
	public static final ConfMaterial BOLLARD_DEFAULT =
		new ConfMaterial(Lighting.FLAT, Color.LIGHT_GRAY);
	
	public static final ConfMaterial BRIDGE_DEFAULT =
		new ConfMaterial(Lighting.FLAT, Color.GRAY);
	public static final ConfMaterial BRIDGE_PILLAR_DEFAULT =
		new ConfMaterial(Lighting.FLAT, Color.GRAY);
	
	public static final ConfMaterial TUNNEL_DEFAULT =
		new ConfMaterial(Lighting.FLAT, Color.GRAY, 0.2f, 0.5f);
	
	public static final ConfMaterial TREE_TRUNK =
		new ConfMaterial(Lighting.FLAT, new Color(0.3f, 0.2f, 0.2f));
	public static final ConfMaterial TREE_CROWN =
		new ConfMaterial(Lighting.SMOOTH, new Color(0, 0.5f, 0));
	
	public static final ConfMaterial ADVERTISING_POSTER =
		new ConfMaterial(Lighting.FLAT, new Color(1, 1, 0.8f));
	
	private static final Map<String, ConfMaterial> surfaceMaterialMap =
		new HashMap<String, ConfMaterial>();
	private static final Map<ConfMaterial, String> fieldNameMap =
		new HashMap<ConfMaterial, String>();
	
	static {
		
		surfaceMaterialMap.put("asphalt", ASPHALT);
		surfaceMaterialMap.put("cobblestone", ASPHALT);
		surfaceMaterialMap.put("compacted", GRAVEL);
		surfaceMaterialMap.put("concrete", CONCRETE);
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
				if (field.getType().equals(ConfMaterial.class)) {
					fieldNameMap.put(
							(ConfMaterial)field.get(null),
							field.getName());
				}
			}
		} catch (Exception e) {
			throw new Error(e);
		}
		
	}

	/** returns all materials defined here */
	public static final Collection<ConfMaterial> getMaterials() {
		return fieldNameMap.keySet();
	}

	/** returns a material defined here based on its field name */
	public static final ConfMaterial getMaterial(String fieldName) {
		for (Entry<ConfMaterial, String> entry : fieldNameMap.entrySet()) {
			if (entry.getValue().equals(fieldName)) {
				return entry.getKey();
			}
		}
		return null;
	}
	
	/** returns a material for a surface value; null if none is found */
	public static final Material getSurfaceMaterial(String value) {
		return getSurfaceMaterial(value, null);
	}
	
	/** same as {@link #getSurfaceMaterial(String)}, but with fallback value */
	public static final Material getSurfaceMaterial(String value,
			Material fallback) {
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
	
	private static final String CONF_KEY_REGEX = "material_(.+)_(color)";
		
	/**
	 * configures the attributes of the materials within this class
	 * based on external configuration settings
	 */
	public static final void configureMaterials(Configuration config) {
		
		// unchecked type parameter necessary due to Apache libs' old interface
		@SuppressWarnings("unchecked")
		Iterator<String> keyIterator = config.getKeys();
		
		while (keyIterator.hasNext()) {
			
			String key = keyIterator.next();
			
			Matcher matcher = Pattern.compile(CONF_KEY_REGEX).matcher(key);
			
			if (matcher.matches()) {
				
				ConfMaterial material = getMaterial(matcher.group(1));
				
				if (material != null) {
				
					String attribute = matcher.group(2);
					
					if ("color".equals(attribute)) {
					
						try {
							material.setColor(
								Color.decode(config.getString(key)));
						} catch (NumberFormatException e) {
							System.err.println("incorrect color value: "
									+ config.getString(key));
						}
						
					} else {
						System.err.println("unknown material attribute: "
								+ attribute);
					}
				
				} else {
					System.err.println("unknown material: " + matcher.group(1));
				}
				
			}
			
		}
		
	}
	
}
