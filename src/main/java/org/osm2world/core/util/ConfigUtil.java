package org.osm2world.core.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;

/**
 * utility class for parsing configuration values
 */
final public class ConfigUtil {

	private ConfigUtil() { }

	public static final String BG_COLOR_KEY = "backgroundColor";
	public static final String BG_IMAGE_KEY = "backgroundImage";
	public static final String CANVAS_LIMIT_KEY = "canvasLimit";

	public static final Color parseColor(String colorString) {

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

}