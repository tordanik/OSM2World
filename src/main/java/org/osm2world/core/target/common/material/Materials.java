package org.osm2world.core.target.common.material;

import static java.awt.Color.*;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.target.common.ImageTextureData;
import org.osm2world.core.target.common.TextTextureData;
import org.osm2world.core.target.common.TextTextureData.FontStyle;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.TextureData.Wrap;
import org.osm2world.core.target.common.material.Material.AmbientOcclusion;
import org.osm2world.core.target.common.material.Material.Interpolation;
import org.osm2world.core.target.common.material.Material.Shadow;
import org.osm2world.core.target.common.material.Material.Transparency;
import org.osm2world.core.util.ConfigUtil;
import org.osm2world.core.world.creation.WorldModule;

/**
 * this class defines materials that can be used by all {@link WorldModule}s
 */
public final class Materials {

	/** prevents instantiation */
	private Materials() {}

	/** material for "empty" ground */
	public static final ConfMaterial TERRAIN_DEFAULT =
		new ConfMaterial(Interpolation.SMOOTH, Color.GREEN);

	public static final ConfMaterial WATER =
		new ConfMaterial(Interpolation.FLAT, Color.BLUE);
	public static final ConfMaterial PURIFIED_WATER =
			new ConfMaterial(Interpolation.FLAT, Color.BLUE);

	public static final ConfMaterial ASPHALT =
		new ConfMaterial(Interpolation.FLAT, new Color(0.3f, 0.3f, 0.3f));
	public static final ConfMaterial BRICK =
		new ConfMaterial(Interpolation.FLAT, new Color(1.0f, 0.5f, 0.25f));
	public static final ConfMaterial COBBLESTONE =
		new ConfMaterial(Interpolation.FLAT, new Color(0.3f, 0.3f, 0.3f));
	public static final ConfMaterial CONCRETE =
		new ConfMaterial(Interpolation.FLAT, new Color(0.55f, 0.55f, 0.55f));
	public static final ConfMaterial EARTH =
		new ConfMaterial(Interpolation.FLAT, new Color(0.3f, 0, 0));
	public static final ConfMaterial GLASS =
		new ConfMaterial(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
	public static final ConfMaterial GRASS =
		new ConfMaterial(Interpolation.FLAT, new Color(0.0f, 0.8f, 0.0f));
	public static final ConfMaterial GRASS_PAVER =
		new ConfMaterial(Interpolation.FLAT, new Color(0.3f, 0.5f, 0.3f));
	public static final ConfMaterial SCRUB =
		new ConfMaterial(Interpolation.FLAT, new Color(0.0f, 0.8f, 0.0f));
	public static final ConfMaterial SETT =
		new ConfMaterial(Interpolation.FLAT, new Color(0.3f, 0.3f, 0.3f));
	public static final ConfMaterial GRAVEL =
		new ConfMaterial(Interpolation.FLAT, new Color(0.4f, 0.4f, 0.4f));
	public static final ConfMaterial PAVING_STONE =
			new ConfMaterial(Interpolation.FLAT, new Color(0.4f, 0.4f, 0.4f));
	public static final ConfMaterial PEBBLESTONE =
			new ConfMaterial(Interpolation.FLAT, new Color(0.4f, 0.4f, 0.4f));
	public static final ConfMaterial PLASTIC =
			new ConfMaterial(Interpolation.FLAT, new Color(255, 255, 255));
	public static final ConfMaterial PLASTIC_GREY =
			new ConfMaterial(Interpolation.FLAT, new Color(184, 184, 184));
	public static final ConfMaterial PLASTIC_BLACK =
			new ConfMaterial(Interpolation.FLAT, new Color(0, 0, 0));
	public static final ConfMaterial SAND =
		new ConfMaterial(Interpolation.FLAT, new Color(241, 233, 80));
	public static final ConfMaterial STEEL =
		new ConfMaterial(Interpolation.FLAT, new Color(200, 200, 200));
	public static final ConfMaterial UNHEWN_COBBLESTONE =
		new ConfMaterial(Interpolation.FLAT, new Color(0.3f, 0.3f, 0.3f));
	public static final ConfMaterial WOOD =
		new ConfMaterial(Interpolation.FLAT, new Color(0.3f, 0.2f, 0.2f));
	public static final ConfMaterial WOOD_WALL =
		new ConfMaterial(Interpolation.FLAT, new Color(0.3f, 0.2f, 0.2f));
	public static final ConfMaterial TARTAN =
		new ConfMaterial(Interpolation.FLAT, new Color(206, 109, 90));

	public static final ConfMaterial ROAD_MARKING =
		new ConfMaterial(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
	public static final ConfMaterial ROAD_MARKING_DASHED =
			new ConfMaterial(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
	public static final ConfMaterial ROAD_MARKING_ZEBRA =
			new ConfMaterial(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
	public static final ConfMaterial ROAD_MARKING_CROSSING =
			new ConfMaterial(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
	public static final ConfMaterial ROAD_MARKING_ARROW_THROUGH =
			new ConfMaterial(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
	public static final ConfMaterial ROAD_MARKING_ARROW_THROUGH_RIGHT =
			new ConfMaterial(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
	public static final ConfMaterial ROAD_MARKING_ARROW_RIGHT =
			new ConfMaterial(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
	public static final ConfMaterial ROAD_MARKING_ARROW_RIGHT_LEFT =
			new ConfMaterial(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
	public static final ConfMaterial RED_ROAD_MARKING =
			new ConfMaterial(Interpolation.FLAT, new Color(0.6f, 0.3f, 0.3f));
	public static final ConfMaterial KERB =
			new ConfMaterial(Interpolation.FLAT, new Color(0.4f, 0.4f, 0.4f));
	public static final ConfMaterial STEPS_DEFAULT =
		new ConfMaterial(Interpolation.FLAT, Color.DARK_GRAY);
	public static final ConfMaterial HANDRAIL_DEFAULT =
		new ConfMaterial(Interpolation.FLAT, Color.LIGHT_GRAY);

	public static final ConfMaterial RAIL_DEFAULT =
		new ConfMaterial(Interpolation.FLAT, Color.LIGHT_GRAY);
	public static final ConfMaterial RAIL_SLEEPER_DEFAULT =
		new ConfMaterial(Interpolation.FLAT, new Color(0.3f, 0.2f, 0.2f));
	public static final ConfMaterial RAIL_BALLAST_DEFAULT =
		new ConfMaterial(Interpolation.FLAT, Color.DARK_GRAY);

	public static final ConfMaterial RUNWAY_CENTER_MARKING =
			new ConfMaterial(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
	public static final ConfMaterial HELIPAD_MARKING =
			new ConfMaterial(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));

	public static final ConfMaterial BUILDING_DEFAULT =
		new ConfMaterial(Interpolation.FLAT, new Color(1f, 0.9f, 0.55f));
	public static final ConfMaterial BUILDING_WINDOWS =
		new ConfMaterial(Interpolation.FLAT, new Color(1f, 0.9f, 0.55f));
	public static final ConfMaterial SINGLE_WINDOW =
			new ConfMaterial(Interpolation.FLAT, WHITE);
	public static final ConfMaterial ROOF_DEFAULT =
		new ConfMaterial(Interpolation.FLAT, new Color(0.8f, 0, 0));
	public static final ConfMaterial GLASS_ROOF =
			new ConfMaterial(Interpolation.FLAT, new Color(0.9f, 0.9f, 0.9f));
	public static final ConfMaterial ENTRANCE_DEFAULT =
		new ConfMaterial(Interpolation.FLAT, new Color(0.2f, 0, 0));
	public static final ConfMaterial GARAGE_DOOR =
			new ConfMaterial(Interpolation.FLAT, WHITE);

	public static final ConfMaterial WALL_DEFAULT =
		new ConfMaterial(Interpolation.FLAT, Color.GRAY);
	public static final ConfMaterial WALL_GABION =
		new ConfMaterial(Interpolation.FLAT, Color.GRAY);

	public static final ConfMaterial HEDGE =
		new ConfMaterial(Interpolation.FLAT, new Color(0,0.5f,0));

	public static final ConfMaterial FENCE_DEFAULT =
		new ConfMaterial(Interpolation.FLAT, new Color(0.3f, 0.2f, 0.2f));
	public static final ConfMaterial SPLIT_RAIL_FENCE =
		new ConfMaterial(Interpolation.FLAT, new Color(0.3f, 0.2f, 0.2f));
	public static final ConfMaterial CHAIN_LINK_FENCE =
		new ConfMaterial(Interpolation.FLAT, new Color(188, 198, 204));
	public static final ConfMaterial METAL_FENCE =
		new ConfMaterial(Interpolation.FLAT, new Color(188, 198, 204));
	public static final ConfMaterial METAL_FENCE_POST =
		new ConfMaterial(Interpolation.FLAT, new Color(188, 198, 204));

	public static final ConfMaterial BRIDGE_DEFAULT =
		new ConfMaterial(Interpolation.FLAT, Color.GRAY);
	public static final ConfMaterial BRIDGE_PILLAR_DEFAULT =
		new ConfMaterial(Interpolation.FLAT, Color.GRAY);

	public static final ConfMaterial TUNNEL_DEFAULT =
		new ConfMaterial(Interpolation.FLAT, Color.GRAY, 0.2f, 0.5f,
				Transparency.FALSE, Collections.<TextureData>emptyList());

	public static final ConfMaterial TREE_TRUNK =
		new ConfMaterial(Interpolation.FLAT, new Color(0.3f, 0.2f, 0.2f));
	public static final ConfMaterial TREE_CROWN =
		new ConfMaterial(Interpolation.SMOOTH, new Color(0, 0.5f, 0));
	public static final ConfMaterial TREE_BILLBOARD_BROAD_LEAVED =
		new ConfMaterial(Interpolation.FLAT, new Color(0, 0.5f, 0), 1f, 0f,
				Transparency.FALSE, Collections.<TextureData>emptyList());
	public static final ConfMaterial TREE_BILLBOARD_BROAD_LEAVED_FRUIT =
		new ConfMaterial(Interpolation.FLAT, new Color(0, 0.5f, 0), 1f, 0f,
				Transparency.FALSE, Collections.<TextureData>emptyList());
	public static final ConfMaterial TREE_BILLBOARD_CONIFEROUS =
		new ConfMaterial(Interpolation.FLAT, new Color(0, 0.5f, 0), 1f, 0f,
				Transparency.FALSE, Collections.<TextureData>emptyList());

	public static final ConfMaterial POWER_TOWER_VERTICAL =
		new ConfMaterial(Interpolation.FLAT, new Color(.7f, .7f, .7f), 1f, 0f,
				Transparency.BINARY, Collections.<TextureData>emptyList());
	public static final ConfMaterial POWER_TOWER_HORIZONTAL =
			new ConfMaterial(Interpolation.FLAT, new Color(.7f, .7f, .7f), 1f, 0f,
					Transparency.BINARY, Collections.<TextureData>emptyList());

	public static final ConfMaterial ADVERTISING_POSTER =
		new ConfMaterial(Interpolation.FLAT, new Color(1, 1, 0.8f));

	public static final ConfMaterial BUS_STOP_SIGN =
		new ConfMaterial(Interpolation.FLAT, new Color(0.98f, 0.90f, 0.05f));

	public static final ConfMaterial GRITBIN_DEFAULT =
			new ConfMaterial(Interpolation.FLAT, new Color(0.3f, 0.5f, 0.4f));

	public static final ConfMaterial POSTBOX_DEUTSCHEPOST =
			new ConfMaterial(Interpolation.FLAT, new Color(1f, 0.8f, 0f));
	public static final ConfMaterial POSTBOX_ROYALMAIL =
			new ConfMaterial(Interpolation.FLAT, new Color(0.8f, 0, 0));
	public static final ConfMaterial TELEKOM_MANGENTA =
			new ConfMaterial(Interpolation.FLAT, new Color(0.883f, 0f, 0.453f));

	public static final ConfMaterial FIREHYDRANT =
		new ConfMaterial(Interpolation.FLAT, new Color(0.8f, 0, 0));

	public static final ConfMaterial FLAGCLOTH =
		new ConfMaterial(Interpolation.SMOOTH, new Color(1f, 1f, 1f));

	public static final ConfMaterial SOLAR_PANEL =
			new ConfMaterial(Interpolation.FLAT, Color.BLUE);

	public static final ConfMaterial PITCH_BEACHVOLLEYBALL =
			new ConfMaterial(Interpolation.FLAT, new Color(241, 233, 80));
	public static final ConfMaterial PITCH_SOCCER =
			new ConfMaterial(Interpolation.FLAT, new Color(0.0f, 0.8f, 0.0f));
	public static final ConfMaterial PITCH_TENNIS_ASPHALT =
			new ConfMaterial(Interpolation.FLAT, new Color(0.3f, 0.3f, 0.3f));
	public static final ConfMaterial PITCH_TENNIS_CLAY =
			new ConfMaterial(Interpolation.FLAT, new Color(0.8f, 0.0f, 0.0f));
	public static final ConfMaterial PITCH_TENNIS_GRASS =
			new ConfMaterial(Interpolation.FLAT, new Color(0.0f, 0.8f, 0.0f));
	public static final ConfMaterial PITCH_TENNIS_SINGLES_ASPHALT =
			new ConfMaterial(Interpolation.FLAT, new Color(0.3f, 0.3f, 0.3f));
	public static final ConfMaterial PITCH_TENNIS_SINGLES_CLAY =
			new ConfMaterial(Interpolation.FLAT, new Color(0.8f, 0.0f, 0.0f));
	public static final ConfMaterial PITCH_TENNIS_SINGLES_GRASS =
			new ConfMaterial(Interpolation.FLAT, new Color(0.0f, 0.8f, 0.0f));

	public static final ConfMaterial TENNIS_NET =
			new ConfMaterial(Interpolation.FLAT, Color.WHITE);

	public static final ConfMaterial SKYBOX =
		new ConfMaterial(Interpolation.FLAT, new Color(0, 0, 1),
				1, 0, Transparency.FALSE, null);

	/** material for "nothingness" which reflects no light. Used e.g. for openings into buildings without indoor. */
	public static final ConfMaterial VOID = new ConfMaterial(Interpolation.FLAT, BLACK);

	private static final Map<String, ConfMaterial> surfaceMaterialMap =
		new HashMap<String, ConfMaterial>();
	private static final Map<ConfMaterial, String> fieldNameMap =
		new HashMap<ConfMaterial, String>();
	private static final Map<String, ConfMaterial> externalMaterials =
		new HashMap<String, ConfMaterial>();

	static {

		surfaceMaterialMap.put("asphalt", ASPHALT);
		surfaceMaterialMap.put("cobblestone", COBBLESTONE);
		surfaceMaterialMap.put("compacted", GRAVEL);
		surfaceMaterialMap.put("concrete", CONCRETE);
		surfaceMaterialMap.put("grass", GRASS);
		surfaceMaterialMap.put("gravel", GRAVEL);
		surfaceMaterialMap.put("grass_paver", GRASS_PAVER);
		surfaceMaterialMap.put("ground", EARTH);
		surfaceMaterialMap.put("paved", ASPHALT);
		surfaceMaterialMap.put("paving_stones", PAVING_STONE);
		surfaceMaterialMap.put("pebblestone", PEBBLESTONE);
		surfaceMaterialMap.put("sand", SAND);
		surfaceMaterialMap.put("sett", SETT);
		surfaceMaterialMap.put("steel", STEEL);
		surfaceMaterialMap.put("tartan", TARTAN);
		surfaceMaterialMap.put("unpaved", EARTH);
		surfaceMaterialMap.put("unhewn_cobblestone", UNHEWN_COBBLESTONE);
		surfaceMaterialMap.put("wood", WOOD);
		surfaceMaterialMap.put("scrub", SCRUB);

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

		/*
		 * If the material is not defined in Materials.java, loop
		 * through externalMaterials map
		 */
		for (String key : externalMaterials.keySet()) {
			if(fieldName.equals(key)) {
				return externalMaterials.get(fieldName);
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

	private static final String CONF_KEY_REGEX =
					"material_(.+)_(diffuseFactor|ambientFactor|interpolation|color|specular|shininess|shadow|ssao|transparency|texture\\d*_(?:file|width|height|bumpmap|colorable|type|text|font|topOffset|leftOffset|textColor|relative_font_size))";

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

				String materialName = matcher.group(1);
				ConfMaterial material = getMaterial(materialName);

			/* If material is not defined in Materials.java, create new material
			   and add it to externalMaterials map */
				if (material == null) {
					material = new ConfMaterial(Interpolation.FLAT, Color.white);
					externalMaterials.put(materialName, material);
				}

				String attribute = matcher.group(2);

				if("interpolation".equals(attribute)) {

					String value = config.getString(key).toUpperCase();
					Interpolation interpolation = Interpolation.valueOf(value);

					if (interpolation != null) {
						material.setInterpolation(interpolation);
					}

				} else if("ambientFactor".equals(attribute)) {

					float ambient = config.getFloat(key);
					material.setAmbientFactor(ambient);

				} else if("diffuseFactor".equals(attribute)) {

					float diffuse = config.getFloat(key);
					material.setDiffuseFactor(diffuse);

				} else if ("color".equals(attribute)) {

					Color color = ConfigUtil.parseColor(
							config.getString(key));

					if (color != null) {
						material.setColor(color);
					} else {
						System.err.println("incorrect color value: "
								+ config.getString(key));
					}

				} else if ("specular".equals(attribute)) {

					float specular = config.getFloat(key);
					material.setSpecularFactor(specular);

				} else if ("shininess".equals(attribute)) {

					int shininess = config.getInt(key);
					material.setShininess(shininess);

				} else if ("shadow".equals(attribute)) {

					String value = config.getString(key).toUpperCase();
					Shadow shadow = Shadow.valueOf(value);

					if (shadow != null) {
						material.setShadow(shadow);
					}

				} else if ("ssao".equals(attribute)) {

					String value = config.getString(key).toUpperCase();
					AmbientOcclusion ao = AmbientOcclusion.valueOf(value);

					if (ao != null) {
						material.setAmbientOcclusion(ao);
					}

				} else if ("transparency".equals(attribute)) {

					String value = config.getString(key).toUpperCase();
					Transparency transparency = Transparency.valueOf(value);

					if (transparency != null) {
						material.setTransparency(transparency);
					}

				} else if (attribute.startsWith("texture")) {

					List<TextureData> textureDataList =
						new ArrayList<TextureData>();

					for (int i = 0; i < 32; i++) {

						String widthKey = "material_" + materialName + "_texture" + i + "_width";
						String heightKey = "material_" + materialName + "_texture" + i + "_height";
						String wrapKey = "material_" + materialName + "_texture" + i + "_wrap";
						String coordFunctionKey = "material_" + materialName + "_texture" + i + "_coord_function";
						String colorableKey = "material_" + materialName + "_texture" + i + "_colorable";
						String bumpmapKey = "material_" + materialName + "_texture" + i + "_bumpmap";

						//get texture layer type
						String typeKey = "material_" + materialName + "_texture" + i + "_type";
						String type = config.getString(typeKey, "image");

							if("text".equals(type)) {

								double defaultWidth = 0.5;
								double defaultHeight = 0.5;

								String fontKey = "material_" + materialName + "_texture" + i + "_font";
								String textKey = "material_" + materialName + "_texture" + i + "_text";
								String topOffsetKey = "material_" + materialName + "_texture" + i + "_topOffset";
								String leftOffsetKey = "material_" + materialName + "_texture" + i + "_leftOffset";
								String relativeFontSizeKey = "material_" + materialName + "_texture" + i + "_relative_font_size";
								String textColorKey = "material_" + materialName + "_texture" + i + "_textColor";

								String text = "";

								//get text configuration
								if(config.getString(textKey) != null) {
									text = config.getString(textKey);
								}

								//get font configuration
								Font font = null;
								if(config.getString(fontKey) == null) {

									font = new Font("Dialog", Font.PLAIN, 100);

								} else {

									String[] values = config.getString(fontKey).split(",", 2);

									if(values.length == 2) {

										int fontStyle = FontStyle.getStyle(values[1].toUpperCase());

										font = new Font(values[0], fontStyle, 100);

									}else {
										font = new Font("Dialog", Font.PLAIN, 100);
									}
								}

								double width = config.getDouble(widthKey, defaultWidth);
								double height = config.getDouble(heightKey, defaultHeight);
								boolean colorable = config.getBoolean(colorableKey, false);
								boolean isBumpMap = config.getBoolean(bumpmapKey, false);

								String wrapString = config.getString(wrapKey);
								Wrap wrap = getWrap(wrapString);

								String coordFunctionString = config.getString(coordFunctionKey);
								TexCoordFunction coordFunction = getCoordFunction(coordFunctionString);

								//get top/left offset configuration
								String topOffset = config.getString(topOffsetKey);
								if(topOffset!=null) {
									if(topOffset.endsWith("%")) {
										topOffset = topOffset.substring(0, topOffset.length() - 1);
									}
								}else {
									topOffset = Integer.toString(50);
								}

								String leftOffset = config.getString(leftOffsetKey);
								if(leftOffset!=null) {
									if(leftOffset.endsWith("%")) {
										leftOffset = leftOffset.substring(0, leftOffset.length() - 1);
									}
								}else {
									leftOffset = Integer.toString(50);
								}

								//get text color configuration
								Color color = Color.BLACK;
								String colorString = config.getString(textColorKey);
								if(colorString!=null) {
									color = ConfigUtil.parseColor(colorString);
									if(color==null) {
										System.err.println("Incorrect color value: "+colorString+". Defaulting to black.");
										color = Color.BLACK;
									}
								}

								//get relative font size
								double relativeFontSize = config.getDouble(relativeFontSizeKey, 60);

								TextTextureData textTextureData = new TextTextureData(text, font, width, height,
										Double.parseDouble(topOffset), Double.parseDouble(leftOffset), color,
										relativeFontSize, wrap, coordFunction, colorable, isBumpMap);

								textureDataList.add(textTextureData);

							} else if("image".equals(type)) {

								String fileKey = "material_" + materialName + "_texture" + i + "_file";

								if (config.getString(fileKey) == null) break;

								File file = new File(config.getString(fileKey));

								double width = config.getDouble(widthKey, 1);
								double height = config.getDouble(heightKey, 1);
								boolean colorable = config.getBoolean(colorableKey, false);
								boolean isBumpMap = config.getBoolean(bumpmapKey, false);

								String wrapString = config.getString(wrapKey);
								Wrap wrap = getWrap(wrapString);

								String coordFunctionString = config.getString(coordFunctionKey);
								TexCoordFunction coordFunction = getCoordFunction(coordFunctionString);

								// bumpmaps are only supported in the shader implementation, skip for others
								if (!isBumpMap || "shader".equals(config.getString("joglImplementation"))) {

									TextureData textureData = new ImageTextureData(
											file, width, height, wrap, coordFunction, colorable, isBumpMap);
									textureDataList.add(textureData);
								}
							} else System.err.println("unknown type value: " + type);
					}

					material.setTextureDataList(textureDataList);

				} else {
					System.err.println("unknown material attribute: "
							+ attribute);
				}
			}
		}
	}



	private static Wrap getWrap(String wrapString) {

		Wrap wrap = Wrap.REPEAT;
		if ("clamp_to_border".equalsIgnoreCase(wrapString)) {
			wrap = Wrap.CLAMP_TO_BORDER;
		} else if ("clamp".equalsIgnoreCase(wrapString)) {
			wrap = Wrap.CLAMP;
		}

		return wrap;
	}

	private static TexCoordFunction getCoordFunction(String coordFunctionString) {

		TexCoordFunction coordFunction = null;
		if (coordFunctionString != null) {
			coordFunction = NamedTexCoordFunction.valueOf(
					coordFunctionString.toUpperCase());
		}

		return coordFunction;
	}

}
