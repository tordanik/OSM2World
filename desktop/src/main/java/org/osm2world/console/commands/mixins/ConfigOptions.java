package org.osm2world.console.commands.mixins;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.osm2world.conversion.O2WConfig;

import picocli.CommandLine;

public class ConfigOptions {

	@CommandLine.Option(names = {"--config"}, description = "properties file(s) with configuration parameters")
	private List<File> config = List.of();

	public static final File STANDARD_PROPERTIES_FILE = new File("standard.properties");

	public List<File> getConfigFiles() {

		if (config.isEmpty() && STANDARD_PROPERTIES_FILE.isFile()) {
			System.out.println("No --config parameter, using default style (" + STANDARD_PROPERTIES_FILE + ").\n");
			return List.of(STANDARD_PROPERTIES_FILE);
		} else {
			return config;
		}

	}

	public O2WConfig getO2WConfig(Map<String, ?> extraProperties) {

		File[] configFiles = getConfigFiles().toArray(new File[0]);

		try {
			return new O2WConfig(extraProperties, configFiles);
		} catch (Exception e) {
			System.err.println("could not read config, ignoring it:\n" + e);
			return new O2WConfig();
		}

	}

}
