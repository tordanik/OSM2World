package org.osm2world.core.conversion;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * utility class for parsing configuration values
 */
final public class ConfigUtil {

	private ConfigUtil() { }

	/**
	 * Registers the fonts that exist in the directory specified
	 * by the "fontDirectory" key in the configuration file.
	 * The respective fonts can then be used in Font object constructors.
	 */
	public static void parseFonts(O2WConfig config) {

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
