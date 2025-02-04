package org.osm2world.core.conversion;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.osm2world.core.target.common.mesh.LevelOfDetail;

/**
 * a set of configuration options for OSM2World.
 * Includes models, materials and settings which control the visual appearance of the scene.
 */
public class O2WConfig {

	private final PropertiesConfiguration config;

	private O2WConfig(PropertiesConfiguration config) {
		this.config = config;
	}

	/**
	 * constructs a configuration with all options at their default values.
	 */
	public O2WConfig() {
		this(Map.of());
	}

	/**
	 * constructs a configuration from a {@link Map} containing key-value options for OSM2World.
	 */
	public O2WConfig(@Nullable Map<String, ?> properties) {
		this(properties, new File[0]);
	}

	/**
	 * constructs a configuration from a {@link Map} containing key-value options for OSM2World
	 * and one or more config files. Config files use the .properties format.
	 * For keys which are present multiple times, values from the map take precedence over values from the config files.
	 * Among configFiles, those later in the list take precedence.
	 */
	public O2WConfig(@Nullable Map<String, ?> properties, File... configFiles) {
		try {
			var mapConfig = new MapConfiguration(properties == null ? Map.of() : properties);
			PropertiesConfiguration loadedConfig = loadConfigFiles(configFiles);
			loadedConfig.copy(mapConfig);
			this.config = loadedConfig;
		} catch (ConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * returns a modified copy of this config that has one key set to a new value.
	 * @param value  the new value; can be set to null to delete an existing property
	 */
	public O2WConfig withProperty(String key, @Nullable Object value) {
		PropertiesConfiguration newConfig = (PropertiesConfiguration) config.clone();
		newConfig.clearProperty(key);
		if (value != null) {
			newConfig.addProperty(key, value);
		}
		return new O2WConfig(newConfig);
	}

	public Iterator<String> getKeys() {
		return config.getKeys();
	}

	public boolean containsKey(String key) {
		return config.containsKey(key);
	}

	public String getString(String key) {
		return config.getString(key);
	}

	public String getString(String key, String defaultValue) {
		return config.getString(key, defaultValue);
	}

	public boolean getBoolean(String key) {
		return config.getBoolean(key);
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		return config.getBoolean(key, defaultValue);
	}

	public int getInt(String key, int defaultValue) {
		return config.getInt(key, defaultValue);
	}

	public Integer getInteger(String key, Integer defaultValue) {
		return config.getInteger(key, defaultValue);
	}

	public float getFloat(String key, float defaultValue) {
		return config.getFloat(key, defaultValue);
	}

	public Float getFloat(String key, Float defaultValue) {
		return config.getFloat(key, defaultValue);
	}

	public double getDouble(String key, double defaultValue) {
		return config.getDouble(key, defaultValue);
	}

	public Double getDouble(String key, Double defaultValue) {
		return config.getDouble(key, defaultValue);
	}

	public List<Object> getList(String key) {
		return config.getList(key);
	}

	public LevelOfDetail getLod() {
		return ConfigUtil.readLOD(this);
	}

	private static PropertiesConfiguration loadConfigFiles(File... configFiles) throws ConfigurationException {

		PropertiesConfiguration config = new PropertiesConfiguration();
		config.setListDelimiter(';');

		for (File it : configFiles) {
			config.load(it);
		}

		Arrays.stream(configFiles)
				.filter(File::exists)
				.findFirst()
				.ifPresent(f -> {
					config.addProperty("configPath", f.getAbsoluteFile().getParent());
				});

		return config;

	}

}
