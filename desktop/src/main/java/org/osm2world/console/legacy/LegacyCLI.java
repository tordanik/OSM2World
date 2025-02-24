package org.osm2world.console.legacy;

import static java.util.Arrays.asList;
import static org.osm2world.GlobalValues.VERSION_STRING;
import static org.osm2world.console.legacy.CLIArgumentsUtil.ProgramMode.CONVERT;
import static org.osm2world.console.legacy.CLIArgumentsUtil.ProgramMode.GUI;
import static org.osm2world.console.legacy.CLIArgumentsUtil.getProgramMode;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import javax.swing.*;

import org.osm2world.GlobalValues;
import org.osm2world.console.commands.ParamsCommand;
import org.osm2world.console.legacy.CLIArgumentsUtil.ProgramMode;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.scene.mesh.LevelOfDetail;
import org.osm2world.viewer.view.ViewerFrame;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.CliFactory;

/**
 * former main class of the OSM2World console application.
 * Will only be called
 */
public class LegacyCLI {

	private static final File STANDARD_PROPERTIES_FILE = new File("standard.properties");

	public static void main(String[] rawArgs) throws Exception {

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
			File parameterFileDir = args.getParameterFileDir();
			if (!parameterFileDir.isDirectory()) {
				System.err.println("parameterFileDir must be a directory!");
			} else {
				ParamsCommand.handleParamFileDir(parameterFileDir, true);
			}
			return;
		}

		/* parse lines from parameter file (if one exists) */

		List<CLIArguments> argumentsList = Collections.singletonList(args);

		if (args.isParameterFile()) {

			argumentsList = new ArrayList<>();

			try {

				List<String[]> unparsedArgsLines = ParamsCommand
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

		LevelOfDetail lod = LevelOfDetail.fromInt(representativeArgs.getLod());

		/* load configuration file */

		O2WConfig config = new O2WConfig();

		try {
			File[] configFiles = representativeArgs.getConfig().toArray(new File[0]);
			Map<String, ?> extraProperties = lod == null ? Map.of() : Map.of("lod", lod.ordinal());
			config = new O2WConfig(extraProperties, configFiles);
		} catch (Exception e) {
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
					LegacyCLIOutput.output(config, argumentsGroup);
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;

			case PARAMFILE:
			case PARAMFILEDIR:
				throw new Error("Cannot recursively execute parameter files. Program mode was: " + programMode);

		}
	}

}