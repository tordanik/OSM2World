package org.osm2world.conversion;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static org.osm2world.scene.mesh.LevelOfDetail.*;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.osm2world.map_elevation.creation.*;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.geo.MetricMapProjection;
import org.osm2world.math.geo.OrthographicAzimuthalMapProjection;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.mesh.LevelOfDetail;
import org.osm2world.style.PropertyStyle;
import org.osm2world.style.Style;
import org.osm2world.util.enums.LeftRight;
import org.osm2world.util.platform.uri.LoadUriUtil;

/**
 * A set of configuration options for OSM2World.
 * Includes models, materials and settings which control the visual appearance of the scene.
 */
public class O2WConfig {

	private final Properties props;

	private Style style;

	private O2WConfig(Properties props) {
		this.props = props;
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
	 * and one or more config files obtained from local or remote URIs. Config files use the .properties format.
	 * For keys which are present multiple times, values from the map take precedence over values from the config files.
	 * Among configFiles, those later in the list take precedence.
	 */
	public O2WConfig(@Nullable Map<String, ?> properties, URI... configFiles) {

		props = new Properties();

		for (URI configFileURI : configFiles) {
			try {
				if (!props.containsKey("configBaseURI")) {
					String basePath = getParentURI(configFileURI).toString();
					props.setProperty("configBaseURI", basePath);
				}
				loadFileWithIncludes(props, configFileURI);
			} catch (IOException | URISyntaxException e) {
				throw new RuntimeException("Error loading configuration file " + configFileURI, e);
			}
		}

		if (properties != null) {
			for (Map.Entry<String, ?> e : properties.entrySet()) {
				if (e.getValue() == null) {
					props.remove(e.getKey());
				} else {
					props.setProperty(e.getKey(), String.valueOf(e.getValue()));
				}
			}
		}

	}

	/**
	 * Variant of {@link #O2WConfig(Map, URI...)} which uses {@link File} objects rather than URIs.
	 */
	public O2WConfig(@Nullable Map<String, ?> properties, File... configFiles) {
		this(properties, stream(configFiles).map(File::toURI).toArray(URI[]::new));
	}

	/**
	 * returns a modified copy of this config that has one key set to a new value.
	 *
	 * @param value the new value; can be set to null to delete an existing property
	 */
	public O2WConfig withProperty(String key, @Nullable Object value) {
		Properties copy = new Properties();
		copy.putAll(this.props);
		if (value == null) {
			copy.remove(key);
		} else {
			copy.setProperty(key, String.valueOf(value));
		}
		return new O2WConfig(copy);
	}

	public Set<String> getKeys() {
		return props.stringPropertyNames();
	}

	public boolean containsKey(String key) {
		return props.containsKey(key);
	}

	public String getString(String key) {
		return props.getProperty(key);
	}

	public String getString(String key, String defaultValue) {
		return props.getProperty(key, defaultValue);
	}

	public boolean getBoolean(String key) {
		return getBoolean(key, false);
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		String v = props.getProperty(key);
		if (v == null) return defaultValue;
		v = v.trim().toLowerCase(Locale.ROOT);
		return v.equals("true") || v.equals("yes") || v.equals("on") || v.equals("1");
	}

	public int getInt(String key, int defaultValue) {
		return getInteger(key, defaultValue);
	}

	public Integer getInteger(String key, Integer defaultValue) {
		String v = props.getProperty(key);
		if (v == null) return defaultValue;
		try {
			return Integer.parseInt(v.trim());
		} catch (NumberFormatException e) {
			try {
				return (int) Math.round(Double.parseDouble(v.trim()));
			} catch (NumberFormatException e2) {
				return defaultValue;
			}
		}
	}

	public float getFloat(String key, float defaultValue) {
		return getFloat(key, (Float) defaultValue);
	}

	public Float getFloat(String key, Float defaultValue) {
		String v = props.getProperty(key);
		if (v == null) return defaultValue;
		try {
			return Float.parseFloat(v.trim());
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	public double getDouble(String key, double defaultValue) {
		return getDouble(key, (Double) defaultValue);
	}

	public Double getDouble(String key, Double defaultValue) {
		String v = props.getProperty(key);
		if (v == null) return defaultValue;
		try {
			return Double.parseDouble(v.trim());
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	public List<String> getList(String key) {
		return getList(key, List.of());
	}

	public List<String> getList(String key, List<String> defaultValue) {
		String v = props.getProperty(key);
		if (v == null) return defaultValue;
		return parseList(v);
	}

	public <T extends Enum<T>> @Nullable T getEnum(Class<T> enumClass, String key) {
		String value = getString(key);
		if (value != null) {
			try {
				return Enum.valueOf(enumClass, value.toUpperCase());
			} catch (IllegalArgumentException ignored) {
			}
		}
		return null;
	}

	public <T extends Enum<T>> EnumSet<T> getEnumSet(Class<T> enumClass, String key, EnumSet<T> defaultValue) {

		List<String> values = getList(key, null);

		if (values == null) {
			return defaultValue;
		}

		EnumSet<T> result = EnumSet.noneOf(enumClass);
		for (String value : values) {
			try {
				result.add(Enum.valueOf(enumClass, value.toUpperCase()));
			} catch (IllegalArgumentException ignored) {
			}
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
	 * the background image for rendered images
	 */
	public @Nullable File backgroundImage() {
		URI uri = resolveFileConfigProperty(getString("backgroundImage"), true, true);
		return uri != null ? new File(uri) : null;
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
		if (containsKey("consoleLogLevels")) {
			return getEnumSet(ConversionLog.LogLevel.class, "consoleLogLevels",
					EnumSet.noneOf(ConversionLog.LogLevel.class));
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
	 * Will also look at tags on way nodes, such as highway=stop and highway=give_way.
	 * The available options are "yes", "limited" and "no".
	 * Experimental option, off by default.
	 */
	public String deduceTrafficSigns() {
		if (containsKey("deduceTrafficSigns")) {
			return getString("deduceTrafficSigns", "no").toLowerCase();
		} else {
			// support old property name for backwards compatibility
			return getString("deduceTrafficSignsFromWayTags", "no").toLowerCase();
		}
	}

	/**
	 * The driving side for roads which have no driving_side tag.
	 */
	public LeftRight drivingSide() {
		return "left".equalsIgnoreCase(getString("drivingSide", "right"))
				? LeftRight.LEFT : LeftRight.RIGHT;
	}

	/**
	 * the algorithm to use for calculating elevations
	 *
	 * @return a function to create an instance of the calculation algorithm
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
		return getBoolean("renderUnderground", true);
	}

	/**
	 * A value which indicates whether input data should be assumed to be at sea rather than on land.
	 * This is necessary because coastline ways will often not be within the bounds of a dataset.
	 *
	 * @return true if the dataset is sea on all sides (it may contain islands as long as they are
	 * entirely within the bounds); false if it's on land or unknown/mixed
	 */
	public boolean isAtSea() {
		return getBoolean("isAtSea", false);
	}

	/**
	 * Can be set to the value "shader" to enable shaders for OpenGL rendering.
	 */
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

	/**
	 * The {@link LevelOfDetail} at which models should be generated.
	 */
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
		URI logDirURI = resolveFileConfigProperty(getString("logDir", null), true, false);
		return logDirURI != null ? new File(logDirURI) : null;
	}

	/**
	 * Returns the map style which should be used to control the visual appearance of the scene
	 */
	public Style mapStyle() {
		if (style == null) {
			style = new PropertyStyle(this);
		}
		return style;
	}

	/**
	 * The type of map projection to use during conversion.
	 *
	 * @return a factory method to create a MapProjection instance from an origin
	 */
	public Function<LatLon, ? extends MapProjection> mapProjection() {
		return switch (getString("mapProjection", "")) {
			case "OrthographicAzimuthalMapProjection" -> OrthographicAzimuthalMapProjection::new;
			default -> MetricMapProjection::new;
		};
	}

	/**
	 * The maximum number of log entries to write to log files.
	 */
	public int maxLogEntries() {
		return getInt("maxLogEntries", 100);
	}

	/**
	 * A directory with locally stored 3DMR models. Models are stored as "id.glb" or "id_version.glb".
	 * If there are multiple versions of a model, the highest version should be used.
	 */
	public @Nullable File model3dmrDir() {
		URI uri = resolveFileConfigProperty(getString("3dmrDir", null), true, true);
		return uri != null ? new File(uri) : null;
	}

	/**
	 * URL prefix for the model API call of a 3DMR instance.
	 * This is used to obtain models which are not available in the {@link #model3dmrDir()}.
	 * The value for the main 3dmr instance would be "https://3dmr.eu/api/model/".
	 */
	public @Nullable String model3dmrUrl() {
		return getString("3dmrUrl", null);
	}

	/**
	 * A directory with SRTM data in .hgt or .hgt.zip format
	 */
	public @Nullable File srtmDir() {
		URI srtmDirURI = resolveFileConfigProperty(getString("srtmDir", null), true, true);
		return srtmDirURI != null ? new File(srtmDirURI) : null;
	}

	/**
	 * The algorithm to use for interpolating terrain elevation between sites of known elevation
	 *
	 * @return a function to create an instance of the algorithm
	 */
	public Supplier<TerrainInterpolator> terrainInterpolator() {
		return switch (getString("terrainInterpolator", "")) {
			case "LinearInterpolator" -> LinearInterpolator::new;
			case "LeastSquaresInterpolator" -> LeastSquaresInterpolator::new;
			case "NaturalNeighborInterpolator" -> NaturalNeighborInterpolator::new;
			case "InverseDistanceWeightingInterpolator" -> InverseDistanceWeightingInterpolator::new;
			default -> ZeroInterpolator::new;
		};
	}

	/**
	 * Image quality for embedded textures.
	 */
	public float textureQuality() {
		return getFloat("textureQuality", 0.75f);
	}

	/**
	 * Default tree density in forests. Large numbers of trees can negatively affect performance.
	 */
	public double treesPerSquareMeter() {
		return getDouble("treesPerSquareMeter", 0.01f);
	}

	/**
	 * if this config references some files by path, e.g. textures,
	 * resolve file paths relative to the location of the config file used to load this config (if any)
	 */
	public @Nullable URI resolveFileConfigProperty(@Nullable String fileName) {
		return resolveFileConfigProperty(fileName, false, true);
	}

	/**
	 * Variant of {@link #resolveFileConfigProperty(String)} which can optionally permit non-existing files.
	 *
	 * @param requireLocalFile  requires that the result is a file ("file" URI scheme)
	 * @param requireFileExists if the result is a local file, requires that the file exists
	 */
	public @Nullable URI resolveFileConfigProperty(@Nullable String fileName, boolean requireLocalFile, boolean requireFileExists) {

		if (fileName == null) {
			return null;
		}

		try {

			URI fileURI = new URI(fileName);

			if (this.containsKey("configBaseURI")) {
				URI base = new URI(this.getString("configBaseURI"));
				fileURI = base.resolve(fileURI);
			} else {
				File file = new File(fileName);
				if (file.isAbsolute()) {
					fileURI = file.toURI();
				}
			}

			if (!"file".equals(fileURI.getScheme())) {
				if (requireLocalFile) {
					System.err.println("File referenced in config is not a local file: " + fileURI);
					return null;
				}
			} else {
				File file = new File(fileURI);
				if (requireFileExists && !file.exists()) {
					System.err.println("File referenced in config does not exist: " + file);
					return null;
				}
			}

			return fileURI;

		} catch (URISyntaxException e) {
			System.err.println("Error resolving file path in config: " + fileName);
			return null;
		}

	}

	private static List<String> parseList(String value) {
		List<String> result = new ArrayList<>();
		for (String part : value.split(";")) {
			String s = part.trim();
			if (!s.isEmpty()) result.add(s);
		}
		return result;
	}

	private static URI getParentURI(URI uri) throws URISyntaxException, MalformedURLException {
		return uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
	}

	private static void loadFileWithIncludes(Properties dest, URI configFileURI) throws IOException, URISyntaxException {

		String configFileContents = LoadUriUtil.fetchText(configFileURI);

		if (configFileContents == null) {
			throw new IOException("Failed to load config file: " + configFileURI);
		}
		
		/* load properties from the file itself */

		Properties current = new Properties();
		try (ByteArrayInputStream inStream = new ByteArrayInputStream(configFileContents.getBytes(UTF_8))) {
			current.load(inStream);
			current.remove("include"); // don't expose include directive
		}

		/* process include directives */

		Pattern includePattern = Pattern.compile("^\\s*include\\s*[:=]\\s*(.+)\\s*$", Pattern.CASE_INSENSITIVE);

		List<String> includeValues = new ArrayList<>();

		new BufferedReader(new StringReader(configFileContents)).lines().forEachOrdered(line -> {
			Matcher m = includePattern.matcher(line);
			if (m.matches()) {
				String includeValue = m.group(1).trim();
				// replace variables enclosed in curly braces
				Pattern variablePattern = Pattern.compile("\\$\\{([^}]+)}");
				Matcher matcher = variablePattern.matcher(includeValue);
				while (matcher.find()) {
					String key = matcher.group(1);
					includeValue = includeValue.replaceAll("\\$\\{" + Pattern.quote(key) + "}", current.getOrDefault(key, "").toString());
					matcher = variablePattern.matcher(includeValue);
				}
				includeValues.add(includeValue);
			}
		});

		for (String includeValue : includeValues) {
			// load properties from the included file
			URI includeFileURI = new URI(includeValue);
			if (!includeFileURI.isAbsolute()) {
				includeFileURI = getParentURI(configFileURI).resolve(includeFileURI);
			}
			loadFileWithIncludes(dest, includeFileURI);
		}

		/* merge properties (override previously loaded values) */

		for (var e : current.entrySet()) {
			dest.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
		}

	}

	public enum ObjectMetadataType {
		ID, TAGS
	}

}
