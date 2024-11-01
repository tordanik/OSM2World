package org.osm2world.core.util;

import static org.osm2world.core.target.common.mesh.LevelOfDetail.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.target.common.mesh.LevelOfDetail;

/**
 * utility class for parsing configuration values
 */
final public class ConfigUtil {

	private ConfigUtil() { }

	public static final String BG_COLOR_KEY = "backgroundColor";
	public static final String BG_IMAGE_KEY = "backgroundImage";
	public static final String CANVAS_LIMIT_KEY = "canvasLimit";

	/** reads and parses the value of the lod property */
	public static LevelOfDetail readLOD(Configuration config) {
		return switch (config.getInt("lod", 4)) {
			case 0 -> LOD0;
			case 1 -> LOD1;
			case 2 -> LOD2;
			case 3 -> LOD3;
			default -> LOD4;
		};
	}

	public static final Color parseColor(@Nullable String colorString, Color defaultValue) {
		Color result = parseColor(colorString);
		return result != null ? result : defaultValue;
	}

	public static final @Nullable Color parseColor(@Nullable String colorString) {

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
	 * 
	 * Currently only supports hsv.
	 * 
	 * @return color; null on parsing errors
	 */
	public static final Color parseColorTuple(String colorString) {

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
	 * Registers the fonts that exist in the directory specified
	 * by the "fontDirectory" key in the configuration file.
	 * The respective fonts can then be used in Font object constructors.
	 */
	public static void parseFonts(Configuration config) {

		if(!config.containsKey("fontDirectory")) return;

		String directoryName = config.getString("fontDirectory");
		File fontDir = new File(directoryName);

		if(!fontDir.isDirectory()) return;

		GraphicsEnvironment gEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();

		File[] listOfFiles = fontDir.listFiles();

		for (File file : listOfFiles) {
		    if (file.isFile()) {

		    	try {
					gEnv.registerFont(Font.createFont(Font.TRUETYPE_FONT, file));
				} catch (FontFormatException | IOException e) {
					System.err.println("Could not register font "+file.getName());
					e.printStackTrace();
				}
		    }
		}
	}

	/**
	 * If config references some files by path i.e. textures
	 * resolve file paths relative to config location
	 */
	public static File resolveFileConfigProperty(Configuration config, String fileName) {
		if (fileName == null) {
			return null;
		}

		String basePath = config.getString("configPath", null);
		File file = new File(basePath, fileName);

		if (!file.exists()) {
			System.err.println("File referenced in config does not exist: " + file);
			return null;
		}

		return file;
	}

}