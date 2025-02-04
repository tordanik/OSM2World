package org.osm2world.core.world.modules.traffic_sign;

import static java.awt.Color.WHITE;
import static org.osm2world.core.target.common.material.Materials.getMaterial;

import javax.annotation.Nullable;

import org.osm2world.core.conversion.O2WConfig;
import org.osm2world.core.target.common.material.ConfMaterial;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Interpolation;

/**
 * a type of traffic sign, characterized by a name (country and id, by OSM convention) and information that might be
 * found in a traffic sign catalog.
 *
 * This has no information about a particular instance of such a traffic sign (such as the text to use for placeholders,
 * or the sign's location or rotation).
 */
public class TrafficSignType {

	public final String name;

	public final Material material;
	public final int defaultNumPosts;
	public final double defaultHeight;

	public TrafficSignType(String name, Material material, int defaultNumPosts, double defaultHeight) {
		this.name = name;
		this.material = material;
		this.defaultNumPosts = defaultNumPosts;
		this.defaultHeight = defaultHeight;
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Parses an {@link O2WConfig} for the traffic sign-specific keys
	 * <code>trafficSign_NAME_numPosts|defaultHeight|material</code>
	 *
	 * @return  a {@link TrafficSignType} with the parsed values; null if the type does not exist or is invalid
	 */
	public static @Nullable TrafficSignType fromConfig(TrafficSignIdentifier sign, O2WConfig config) {
		TrafficSignType result = fromConfig(sign.configKey(), config);
		if (result == null) {
			result = fromConfig(sign.configKeyWithoutSubType(), config);
		}
		return result;
	}

	private static @Nullable TrafficSignType fromConfig(String configKey, O2WConfig config) {

		String keyPrefix = "trafficSign_" + configKey;

		int numPosts = config.getInt(keyPrefix + "_numPosts", 1);

		double defaultHeight = config.getDouble(keyPrefix + "_defaultHeight",
				config.getFloat("defaultTrafficSignHeight", 2));

		String materialName = config.getString(keyPrefix + "_material", configKey).toUpperCase();
		Material material = getMaterial(materialName);

		if (material == null) {
			return null;
		} else {
			return new TrafficSignType(configKey, material, numPosts, defaultHeight);
		}

	}

	public static TrafficSignType blankSign() {
		return new TrafficSignType("", new ConfMaterial(Interpolation.FLAT, WHITE), 1, 2);
	}

}
