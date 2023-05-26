package org.osm2world.console;

import static org.osm2world.console.CLIArgumentsUtil.ProgramMode.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.lexicalscope.jewel.cli.CliFactory;

public final class CLIArgumentsUtil {

	public static enum ProgramMode {GUI, CONVERT, HELP, VERSION, PARAMFILE, PARAMFILEDIR}
	public static enum OutputMode {OBJ, GLTF, GLB, POV, WEB_PBF, PNG, PPM, GD}
	public static enum InputMode {FILE, OVERPASS}
	public static enum InputFileType {SIMPLE_FILE, MBTILES, GEODESK}

	private CLIArgumentsUtil() { }

	public static final boolean isValid(CLIArguments args) {
		return getErrorString(args) == null;
	}

	public static final String getErrorString(CLIArguments args) {

		if (getProgramMode(args) == CONVERT) {

			switch (args.getInputMode()) {

			case FILE:
				if (!args.isInput()) {
					return "input file parameter is required (or choose a different input mode)";
				} else if (getInputFileType(args) != InputFileType.SIMPLE_FILE && !args.isTile()) {
					return "the --tile parameter is required for database input files";
				}
				break;

			case OVERPASS:
				if (!args.isInputQuery() && !args.isInputBoundingBox() && !args.isTile()) {
					return "either a bounding box, a tile, or a query string is required for Overpass";
				}
				break;

			}

			if (!args.isOutput()) {
				return "output file parameter is missing";
			}

			if (args.isOviewTiles() && args.getOviewTiles().isEmpty()) {
				return "at least one tile required";
			}

			if (args.isOviewBoundingBox()
					&& args.getOviewBoundingBox().size() < 2) {
				return "bounding box requires at least two lat,lon pairs";
			}

			if (args.isOviewTiles() && args.isOviewBoundingBox()) {
				return "define *either* tiles or bounding box for" +
						" orthographic view";
			}

			for (File outputFile : args.getOutput()) {
				if (getOutputMode(outputFile) == null) {
					return "cannot identify file type from name " + outputFile
						+ "\navailable output types: " + OutputMode.values();
				}
			}

			if ((args.isPviewPos() && !args.isPviewLookat())
					|| (args.isPviewLookat() && !args.isPviewPos())) {
				return "camera position and look-at for perspective view "
					+ "cannot be used separately, both must be defined";
			}

			if (hasOrthographicArg(args) && hasPerspectiveArg(args)) {
				return "you cannot combine arguments for perspective view "
					+ "and orthographic view";
			}

		}

		return null;

	}

	private static final boolean hasOrthographicArg(CLIArguments args) {
		return args.isOviewBoundingBox()
			|| args.isOviewTiles();
	}

	private static final boolean hasPerspectiveArg(CLIArguments args) {
		return args.isPviewLookat()
			|| args.isPviewPos();
	}

	public static final ProgramMode getProgramMode(CLIArguments args) {
		return args.isParameterFileDir() ? PARAMFILEDIR
				: args.isParameterFile() ? PARAMFILE
					: args.getHelp() ? HELP
						: args.getVersion() ? VERSION
							: args.getGui() ? GUI
								: CONVERT;
	}

	/** equivalent of {@link #getProgramMode(CLIArguments)} for a list-wrapped args array */
	public static @Nullable ProgramMode getProgramMode(List<String> unparsedArgs) {
		try {
			String[] args = unparsedArgs.toArray(new String[0]);
			CLIArguments cliArgs = CliFactory.parseArguments(CLIArguments.class, args);
			return getProgramMode(cliArgs);
		} catch (Exception e) {
			return null;
		}
	}

	/** @param args  set of arguments with non-null {@link CLIArguments#getInput()} */
	public static @Nonnull InputFileType getInputFileType(CLIArguments args) {

		assert args.getInput() != null;

		String inputName = args.getInput().getName();

		if (inputName.endsWith(".mbtiles")) {
			return InputFileType.MBTILES;
		} else if (inputName.endsWith(".gol")) {
			return InputFileType.GEODESK;
		} else {
			return InputFileType.SIMPLE_FILE;
		}

	}

	public static final OutputMode getOutputMode(File outputFile) {
		if (outputFile.getName().toLowerCase().endsWith(".obj")) {
			return OutputMode.OBJ;
		} else if (outputFile.getName().toLowerCase().endsWith(".gltf")) {
			return OutputMode.GLTF;
		} else if (outputFile.getName().toLowerCase().endsWith(".glb")) {
			return OutputMode.GLB;
		} else if (outputFile.getName().toLowerCase().endsWith(".pov")) {
			return OutputMode.POV;
		} else if (outputFile.getName().toLowerCase().endsWith(".o2w.pbf")) {
			return OutputMode.WEB_PBF;
		} else if (outputFile.getName().toLowerCase().endsWith(".png")) {
			return OutputMode.PNG;
		} else if (outputFile.getName().toLowerCase().endsWith(".ppm")) {
			return OutputMode.PPM;
		} else if (outputFile.getName().toLowerCase().endsWith(".gd")) {
			return OutputMode.GD;
		} else {
			return null;
		}
	}

	public static final List<String[]> getUnparsedParameterGroups(
			File parameterFile) throws IOException {

		try (BufferedReader in = new BufferedReader(new FileReader(parameterFile))) {

			List<String[]> result = new ArrayList<>();

			String line;

			while ((line = in.readLine()) != null) {

				if (line.startsWith("#")) continue;
				if (line.trim().isEmpty()) continue;

				List<String> argList = new ArrayList<>();

				Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
				Matcher matcher = regex.matcher(line);

				while (matcher.find()) {
				    if (matcher.group(1) != null) {
				        // Add double-quoted string without the quotes
				    	argList.add(matcher.group(1));
				    } else if (matcher.group(2) != null) {
				        // Add single-quoted string without the quotes
				    	argList.add(matcher.group(2));
				    } else {
				        // Add unquoted word
				    	argList.add(matcher.group());
				    }
				}

				result.add(argList.toArray(new String[argList.size()]));

			}

			return result;

		}

	}

}
