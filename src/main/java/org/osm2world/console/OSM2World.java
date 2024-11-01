package org.osm2world.console;

import static java.util.Arrays.asList;
import static org.osm2world.console.CLIArgumentsUtil.getProgramMode;
import static org.osm2world.console.CLIArgumentsUtil.ProgramMode.CONVERT;
import static org.osm2world.console.CLIArgumentsUtil.ProgramMode.GUI;
import static org.osm2world.core.GlobalValues.VERSION_STRING;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.swing.UIManager;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.osm2world.console.CLIArgumentsUtil.ProgramMode;
import org.osm2world.core.GlobalValues;
import org.osm2world.core.target.common.mesh.LevelOfDetail;
import org.osm2world.core.util.ConfigUtil;
import org.osm2world.viewer.view.ViewerFrame;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.CliFactory;

/**
 * main class of the OSM2World console application
 */
public class OSM2World {

	private static final File STANDARD_PROPERTIES_FILE = new File("standard.properties");

	public static void main(String[] rawArgs) {

		/* assume --gui and standard properties if the respective parameters are missing */

		List<String> unparsedArgs = new ArrayList<>(asList(rawArgs));

		if (unparsedArgs.isEmpty()) {
			System.out.println("No parameters, running graphical interface.\n"
					+ "If you want to use the command line, use the --help"
					+ " parameter for a list of available parameters.");
			unparsedArgs.add("--gui");
		}

		if (!unparsedArgs.contains("--config") && STANDARD_PROPERTIES_FILE.isFile()
				&& Stream.of(CONVERT, GUI).anyMatch(it -> it == getProgramMode(unparsedArgs))) {
			System.out.println("No --config parameter, using default style (" + STANDARD_PROPERTIES_FILE + ").\n");
			unparsedArgs.addAll(asList("--config", STANDARD_PROPERTIES_FILE.toString()));
		}

		/* parse command line arguments */

		CLIArguments args = null;

		try {
			args = parseArguments(unparsedArgs.toArray(new String[0]));
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}

		/* check for parameter file directory mode */

		if (args.isParameterFileDir()) {
			ParamFileDirMode.run(args.getParameterFileDir());
			return;
		}

		/* parse lines from parameter file (if one exists) */

		List<CLIArguments> argumentsList = Collections.singletonList(args);

		if (args.isParameterFile()) {

			argumentsList = new ArrayList<CLIArguments>();

			try {

				List<String[]> unparsedArgsLines = CLIArgumentsUtil
					.getUnparsedParameterGroups(args.getParameterFile());

				for (String[] unparsedArgsLine : unparsedArgsLines) {

					try {
						argumentsList.add(parseArguments(unparsedArgsLine));
					} catch (Exception e) {
						System.err.println("Could not parse parameters from file:");
						System.err.println(Arrays.toString(unparsedArgsLine));
						System.err.println("Ignoring it. Reason:");
						System.err.println(e.getMessage());
					}

				}

			} catch (IOException e) {
				System.err.println(e.getMessage());
				System.exit(1);
			}

		}

		/* collect parameter groups into compatible groups
		 * (groups of parameter groups that use the same input and config files) */

		List<CLIArgumentsGroup> argumentsGroups = new ArrayList<CLIArgumentsGroup>();

		for (CLIArguments arguments : argumentsList) {

			boolean added = false;

			for (CLIArgumentsGroup compatibleGroup : argumentsGroups) {
				if (compatibleGroup.isCompatible(arguments)) {
					// add to existing compatible group
					compatibleGroup.addCLIArguments(arguments);
					added = true;
					break;
				}
			}

			if (!added) {
				// start a new compatible group
				argumentsGroups.add(new CLIArgumentsGroup(arguments));
			}

		}

		/* execute conversions */

		if (argumentsGroups.isEmpty()) {
			System.err.println("warning: empty parameter file, doing nothing");
		}

		for (CLIArgumentsGroup argumentsGroup : argumentsGroups) {

			if (argumentsList.size() > 1) {
				System.out.print("executing conversion for these parameter lines: ");
				for (CLIArguments p : argumentsGroup.getCLIArgumentsList()) {
					System.out.print(argumentsList.indexOf(p) + " ");
				}
				System.out.print("\n");
			}

			executeArgumentsGroup(argumentsGroup);

		}

	}

	private static CLIArguments parseArguments(String[] unparsedArgs)
		throws ArgumentValidationException, Exception {

		CLIArguments args = CliFactory.parseArguments(CLIArguments.class, unparsedArgs);

		if (!CLIArgumentsUtil.isValid(args)) {
			throw new Exception(CLIArgumentsUtil.getErrorString(args));
		}
		return args;

	}

	private static void executeArgumentsGroup(CLIArgumentsGroup argumentsGroup) {

		CLIArguments representativeArgs = argumentsGroup.getRepresentative();

		LevelOfDetail lod = null;
		if (representativeArgs.getLod() != null) {
			lod = LevelOfDetail.values()[representativeArgs.getLod()];
		}

		/* load configuration file */

		Configuration config = new BaseConfiguration();

		try {
			File[] configFiles = representativeArgs.getConfig().toArray(new File[0]);
			config = loadConfigFiles(lod, configFiles);
		} catch (ConfigurationException e) {
			System.err.println("could not read config, ignoring it:\n" + e);
		}

		/* run selected mode */

		ProgramMode programMode = getProgramMode(representativeArgs);

		switch (programMode) {

		case HELP:
			System.out.println(
					CliFactory.createCli(CLIArguments.class).getHelpMessage()
					+ "\n\nFor more information, see " + GlobalValues.WIKI_URI);
			break;

		case VERSION:
			System.out.println("OSM2World " + VERSION_STRING);
			break;

		case GUI:
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch(Exception e) {
				System.out.println("Error setting native look and feel: " + e);
			}
			File input = representativeArgs.isInput() ?
					representativeArgs.getInput() : null;
			new ViewerFrame(config, lod, representativeArgs.getConfig(), input).setVisible(true);
			break;

		case CONVERT:
			try {
				Output.output(config, argumentsGroup);
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;

		case PARAMFILE:
		case PARAMFILEDIR:
			throw new Error("Cannot recursively execute parameter files. Program mode was: " + programMode);

		}
	}

	public static Configuration loadConfigFiles(@Nullable LevelOfDetail lod, File... configFiles)
			throws ConfigurationException {

		PropertiesConfiguration config = new PropertiesConfiguration();
		config.setListDelimiter(';');

		for (File it : configFiles) {
			config.load(it);
		}

		Arrays.stream(configFiles)
			.filter(f -> f.exists())
			.findFirst()
			.ifPresent(f -> {
				config.addProperty("configPath", f.getParent());
			});

		if (lod != null) {
			config.clearProperty("lod");
			config.addProperty("lod", lod.ordinal());
		}

		ConfigUtil.parseFonts(config);

		return config;

	}

}