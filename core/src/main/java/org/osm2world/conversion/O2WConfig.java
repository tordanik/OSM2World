package org.osm2world.conversion;

import static org.osm2world.scene.mesh.LevelOfDetail.*;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
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
import org.osm2world.scene.color.Color;
import org.osm2world.scene.mesh.LevelOfDetail;
import org.osm2world.util.enums.LeftRight;

/**
 * A set of configuration options for OSM2World.
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
	 * Constructs a configuration from a {@link Map} containing key-value options for OSM2World
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

	public List<Object> getList(String key, List<Object> defaultValue) {
		return config.getList(key, defaultValue);
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

	public <T extends Enum<T>> EnumSet<T> getEnumSet(Class<T> enumClass, String key, EnumSet<T> defaultValue) {

		List<Object> values = getList(key, null);

		if (values == null) {
			return defaultValue;
		}

		EnumSet<T> result = EnumSet.noneOf(enumClass);
		for (Object value : values) {
			try {
				result.add(Enum.valueOf(enumClass, value.toString().toUpperCase()));
			} catch (IllegalArgumentException ignored) {}
		}
		return result;

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
	 * Parses colors that are given as a color scheme identifier
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

	/**
	 * the background color for rendered images
	 */
	public Color backgroundColor() {
		return getColor("backgroundColor", Color.BLACK);
	}

	/**
	 * Limit for the size of the canvas used for rendering an exported image.
	 * The width and the height must each not exceed this value.
	 * If the requested image is larger, it may be rendered in multiple passes and combined afterward.
	 * This is intended to avoid overwhelmingly large canvases (which would lead to crashes).
	 */
	public int canvasLimit() {
		return getInt("canvasLimit", 1024);
	}

	/**
	 * The severity levels at which events should be logged to the console.
	 * If this has not been set explicitly, the defaults depend on whether a {@link #logDir()} has been configured.
	 */
	public EnumSet<ConversionLog.LogLevel> consoleLogLevels() {
		if (config.containsKey("consoleLogLevels")) {
			List<ConversionLog.LogLevel> levels = new ArrayList<>();
			for (Object level : config.getList("consoleLogLevels")) {
				try {
					levels.add(ConversionLog.LogLevel.valueOf(level.toString().toUpperCase()));
				} catch (IllegalArgumentException ignored) { }
			}
			return EnumSet.copyOf(levels);
		} else {
			if (logDir() != null) {
				return EnumSet.of(ConversionLog.LogLevel.FATAL);
			} else {
				return EnumSet.allOf(ConversionLog.LogLevel.class);
			}
		}
	}

	/**
	 * Whether the geometry should be clipped to the bounds when exporting to output formats such as glTF.
	 */
	public boolean clipToBounds() {
		return getBoolean("clipToBounds", false);
	}

	/**
	 * Whether traffic signs should be deduced from a way's tags and placed next to the way.
	 * The available options are "yes", "limited" and "no".
	 * Experimental option, off by default.
	 */
	public String deduceTrafficSignsFromWayTags() {
		return getString("deduceTrafficSignsFromWayTags", "no").toLowerCase();
	}

	/**
	 * The driving side for roads which have no driving_side tag.
	 */
	public LeftRight drivingSide() {
		return "left".equalsIgnoreCase(config.getString("drivingSide", "right"))
				? LeftRight.LEFT : LeftRight.RIGHT;
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
	 * Indicates which metadata (such as OSM IDs and tags from the source data) should be exported to the output file.
	 * Only works with some output formats (currently glTF and glb).
	 * By default, only OSM IDs are exported.
	 */
	public Set<ObjectMetadataType> exportMetadata() {
		return getEnumSet(ObjectMetadataType.class, "exportMetadata", EnumSet.of(ObjectMetadataType.ID));
	}

	public boolean forceUnbufferedPNGRendering() {
		return getBoolean("forceUnbufferedPNGRendering", false);
	}

	/**
	 * whether underground {@link org.osm2world.world.data.WorldObject}s should be rendered
	 */
	public boolean renderUnderground() {
		return config.getBoolean("renderUnderground", true);
	}

	/**
	 * A value which indicates whether input data should be assumed to be at sea rather than on land.
	 * This is necessary because coastline ways will often not be within the bounds of a dataset.
	 * @return  true if the dataset is sea on all sides (it may contain islands as long as they are
	 * 	        entirely within the bounds); false if it's on land or unknown/mixed
	 */
	public boolean isAtSea() {
		return getBoolean("isAtSea", false);
	}

	/** Can be set to the value "shader" to enable shaders for OpenGL rendering. */
	public String joglImplementation() {
		return getString("joglImplementation");
	}

	/**
	 * Whether OSM elements will be exported as separate entities to output formats such as glTF.
	 * Disabling this significantly improves performance by allowing geometry to be merged into larger meshes
	 * across object boundaries.
	 */
	public boolean keepOsmElements() {
		return getBoolean("keepOsmElements", true);
	}

	/** The {@link LevelOfDetail} at which models should be generated. */
	public LevelOfDetail lod() {
		return switch (this.getInt("lod", 4)) {
			case 0 -> LOD0;
			case 1 -> LOD1;
			case 2 -> LOD2;
			case 3 -> LOD3;
			default -> LOD4;
		};
	}

	/**
	 * output directory for log files
	 */
	public @Nullable File logDir() {
		return resolveFileConfigProperty(config.getString("logDir", null), false);
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

	/** The maximum number of log entries to write to log files. */
	public int maxLogEntries() {
		return getInt("maxLogEntries", 100);
	}

	/**
	 * A directory with locally stored 3DMR models. Models are stored as "id.glb" or "id_version.glb".
	 * If there are multiple versions of a model, the highest version should be used.
	 */
	public @Nullable File model3dmrDir() {
		return resolveFileConfigProperty(getString("3dmrDir", null));
	}

	/**
	 * URL prefix for the model API call of a 3DMR instance.
	 * This is used to obtain models which are not available in the {@link #model3dmrDir()}.
	 * The value for the main 3dmr instance would be "https://3dmr.eu/api/model/".
	 */
	public @Nullable String model3dmrUrl() {
		return getString("3dmrUrl", null);
	}

	/** A directory with SRTM data in .hgt or .hgt.zip format */
	public @Nullable File srtmDir() {
		return resolveFileConfigProperty(config.getString("srtmDir", null));
	}

	/**
	 * The algorithm to use for interpolating terrain elevation between sites of known elevation
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

	/** Image quality for embedded textures. */
	public float textureQuality() {
		return getFloat("textureQuality", 0.75f);
	}

	/** Default tree density in forests. Large numbers of trees can negatively affect performance. */
	public double treesPerSquareMeter() {
		return getDouble("treesPerSquareMeter", 0.01f);
	}

	/**
	 * if this config references some files by path, e.g. textures,
	 * resolve file paths relative to the location of the config file used to load this config (if any)
	 */
	public @Nullable File resolveFileConfigProperty(@Nullable String fileName) {
		return resolveFileConfigProperty(fileName, true);
	}

	/**
	 * Variant of {@link #resolveFileConfigProperty(String)} which can optionally permit non-existing files.
	 */
	public @Nullable File resolveFileConfigProperty(@Nullable String fileName, boolean requireFileExists) {

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

		if (requireFileExists && !file.exists()) {
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
				.ifPresent(f -> config.addProperty("configPath", f.getAbsoluteFile().getParent()));

		return config;

	}

	public enum ObjectMetadataType {
		ID, TAGS
	}

}
