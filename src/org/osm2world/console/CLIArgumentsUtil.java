package org.osm2world.console;

import static org.osm2world.console.CLIArgumentsUtil.ProgramMode.*;

import java.io.File;

public final class CLIArgumentsUtil {
	
	public static enum ProgramMode {GUI, CONVERT, HELP, VERSION};
	public static enum OutputMode {OBJ, POV, PNG};
	
	private CLIArgumentsUtil() { }
	
	public static final boolean isValid(CLIArguments args) {
		return getErrorString(args) == null;
	}
	
	public static final String getErrorString(CLIArguments args) {
		
		if (getProgramMode(args) == CONVERT) {
			
			if (!args.isInput() || !args.isOutput()) {
				return "input and output are required arguments" +
						" for a conversion";
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
		return args.getHelp() ? HELP
				: args.getVersion() ? VERSION
					: args.getGui() ? GUI
						: CONVERT;
	}
	
	public static final OutputMode getOutputMode(File outputFile) {
		if (outputFile.getName().toLowerCase().endsWith(".obj")) {
			return OutputMode.OBJ;
		} else if (outputFile.getName().toLowerCase().endsWith(".pov")) {
			return OutputMode.POV;
		} else if (outputFile.getName().toLowerCase().endsWith(".png")) {
			return OutputMode.PNG;
		} else {
			return null;
		}
	}
	
}
