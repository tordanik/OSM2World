package org.osm2world.conversion;

import static org.osm2world.target.common.mesh.LevelOfDetail.*;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.osm2world.map_elevation.creation.*;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.geo.MetricMapProjection;
import org.osm2world.math.geo.OrthographicAzimuthalMapProjection;
import org.osm2world.target.common.mesh.LevelOfDetail;

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

	public <T extends Enum<T>> @Nullable T getEnum(Class<T> enumClass, String key) {
		String value = getString(key);
		if (value != null) {
			try {
				return Enum.valueOf(enumClass, value.toUpperCase());
			} catch (IllegalArgumentException ignored) {}
		}
		return null;
	}

	public @Nullable Color getColor(String key) {
		return parseColor(getString(key));
	}

	public Color getColor(String key, Color defaultValue) {
		Color result = getColor(key);
		return result != null ? result : defaultValue;
	}

	private static @Nullable Color parseColor(@Nullable String colorString) {

		if (colorString == null) {
			return null;
		}

		Color color = parseColorTuple(colorString);

		if (color != null) {
			return color;
		} else {

			try {
				return Color.decode(colorString);
			} catch (NumberFormatException e) {
				return null;
			}

		}

	}

	private static final Pattern hsvTuplePattern = Pattern.compile(
			"^hsv\\s*\\(\\s*(\\d{1,3})\\s*," +
					"\\s*(\\d{1,3})\\s*%\\s*," +
					"\\s*(\\d{1,3})\\s*%\\s*\\)");

	/**
	 * parses colors that are given as a color scheme identifier
	 * with a value tuple in brackets.
	 * Currently only supports hsv.
	 *
	 * @return color; null on parsing errors
	 */
	public static Color parseColorTuple(String colorString) {

		Matcher matcher = hsvTuplePattern.matcher(colorString);

		if (matcher.matches()) {

			try {

				int v1 = Integer.parseInt(matcher.group(1));
				int v2 = Integer.parseInt(matcher.group(2));
				int v3 = Integer.parseInt(matcher.group(3));

				return Color.getHSBColor(v1 / 360f, v2 / 100f, v3 / 100f);

			} catch (NumberFormatException nfe) {
				return null;
			}

		}

		return null;

	}

	/** parses and returns the value of the lod property */
	public LevelOfDetail getLod() {
		return switch (this.getInt("lod", 4)) {
			case 0 -> LOD0;
			case 1 -> LOD1;
			case 2 -> LOD2;
			case 3 -> LOD3;
			default -> LOD4;
		};
	}

	/**
	 * the background color for rendered images
	 */
	public Color backgroundColor() {
		return getColor("backgroundColor", Color.BLACK);
	}

	/**
	 * Limit for the size of the canvas used for rendering an exported image.
	 * The width and height each must not exceed this value.
	 * If the requested image is larger, it may be rendered in multiple passes and combined afterward.
	 * This is intended to avoid overwhelmingly large canvases (which would lead to crashes).
	 */
	public int canvasLimit() {
		return getInt("canvasLimit", 1024);
	}

	/**
	 * the algorithm to use for calculating elevations
	 * @return  a function to create an instance of the calculation algorithm
	 */
	public Supplier<EleCalculator> eleCalculator() {
		return switch (getString("eleCalculator", "")) {
			case "NoOpEleCalculator" -> NoOpEleCalculator::new;
			case "EleTagEleCalculator" -> EleTagEleCalculator::new;
			case "ConstraintEleCalculator" -> () -> new ConstraintEleCalculator(new SimpleEleConstraintEnforcer());
			default -> BridgeTunnelEleCalculator::new;
		};
	}

	/**
	 * the algorithm to use for interpolating terrain elevation between sites of known elevation
	 * @return  a function to create an instance of the algorithm
	 */
	public Supplier<TerrainInterpolator> terrainInterpolator() {
		return switch (config.getString("terrainInterpolator", "")) {
			case "LinearInterpolator" -> LinearInterpolator::new;
			case "LeastSquaresInterpolator" -> LeastSquaresInterpolator::new;
			case "NaturalNeighborInterpolator" -> NaturalNeighborInterpolator::new;
			case "InverseDistanceWeightingInterpolator" -> InverseDistanceWeightingInterpolator::new;
			default -> ZeroInterpolator::new;
		};
	}

	/**
	 * The type of map projection to use during conversion.
	 *
	 * @return  a factory method to create a MapProjection instance from an origin
	 */
	public Function<LatLon,? extends MapProjection> mapProjection() {
		return switch (getString("mapProjection", "")) {
			case "OrthographicAzimuthalMapProjection" -> OrthographicAzimuthalMapProjection::new;
			default -> MetricMapProjection::new;
		};
	}

	/**
	 * if this config references some files by path, e.g. textures,
	 * resolve file paths relative to the location of the config file used to load this config (if any)
	 */
	public File resolveFileConfigProperty(String fileName) {

		if (fileName == null) {
			return null;
		}

		File file = new File(fileName);

		String basePath = null;
		if (this.containsKey("configPath")) {
			basePath = this.getString("configPath");
		}

		if (basePath == null && config.getFile() != null) {
			basePath = config.getFile().getAbsoluteFile().getParent();
		}

		if (basePath != null) {
			file = Path.of(basePath).normalize()
					.resolve(Path.of(fileName).normalize()).toFile();
		}

		if (!file.exists()) {
			System.err.println("File referenced in config does not exist: " + file);
			return null;
		}

		return file;

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
