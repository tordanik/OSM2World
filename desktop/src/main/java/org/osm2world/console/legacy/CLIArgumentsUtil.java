package org.osm2world.console.legacy;

import static java.lang.Math.*;
import static org.osm2world.console.legacy.CLIArgumentsUtil.ProgramMode.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osm2world.console.commands.mixins.CameraOptions;
import org.osm2world.math.geo.LatLonBounds;
import org.osm2world.osm.creation.*;
import org.osm2world.util.Resolution;

import com.lexicalscope.jewel.cli.CliFactory;

final class CLIArgumentsUtil {

	public static enum ProgramMode {GUI, CONVERT, HELP, VERSION, PARAMFILE, PARAMFILEDIR}
	public static enum OutputMode {OBJ, GLTF, GLB, GLTF_GZ, GLB_GZ, POV, WEB_PBF, WEB_PBF_GZ, PNG, PPM, GD}
	public static enum InputFileType {SIMPLE_FILE, JSON, MBTILES, GEODESK}

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
				} else if (!List.of(InputFileType.SIMPLE_FILE, InputFileType.JSON).contains(getInputFileType(args))
						&& !args.isTile()) {
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
		} else if (inputName.endsWith(".json")) {
			return InputFileType.JSON;
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
		} else if (outputFile.getName().toLowerCase().endsWith(".gltf.gz")) {
			return OutputMode.GLTF_GZ;
		} else if (outputFile.getName().toLowerCase().endsWith(".glb.gz")) {
			return OutputMode.GLB_GZ;
		} else if (outputFile.getName().toLowerCase().endsWith(".pov")) {
			return OutputMode.POV;
		} else if (outputFile.getName().toLowerCase().endsWith(".o2w.pbf")) {
			return OutputMode.WEB_PBF;
		} else if (outputFile.getName().toLowerCase().endsWith(".o2w.pbf.gz")) {
			return OutputMode.WEB_PBF_GZ;
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

	public static Resolution getResolution(CLIArguments args) {

		double aspectRatio = hasPerspectiveArg(args)
				? args.isPviewAspect() ? args.getPviewAspect() : CameraOptions.DEFAULT_ASPECT_RATIO
				: 1.0 / sin(toRadians(args.getOviewAngle()));

		return args.isResolution() ? args.getResolution()
				: new Resolution(800, (int) round(800 / aspectRatio));

	}

	public static OSMDataReader getOsmDataReader(CLIArguments args) {

		switch (args.getInputMode()) {

			case FILE -> {
				File inputFile = args.getInput();
				return switch (CLIArgumentsUtil.getInputFileType(args)) {
					case SIMPLE_FILE -> new OSMFileReader(inputFile);
					case JSON -> new JsonFileReader(inputFile);
					case MBTILES -> new MbtilesReader(inputFile);
					case GEODESK -> new GeodeskReader(inputFile);
				};
			}

			case OVERPASS -> {
				return new OverpassReader(args.getOverpassURL());
			}

			default -> throw new IllegalStateException("unknown input mode " + args.getInputMode());

		}

	}

	public static OSMDataReaderView getOsmDataView(CLIArguments args) throws IOException {

		OSMDataReader dataReader = CLIArgumentsUtil.getOsmDataReader(args);

		if (args.isInputBoundingBox()) {
			return new OSMDataReaderView(dataReader, LatLonBounds.ofPoints(args.getInputBoundingBox()));
		} else if (args.isTile()) {
			return new OSMDataReaderView(dataReader, args.getTile());
		} else if (args.isInputQuery() && dataReader instanceof OverpassReader overpassReader) {
			return new OSMDataReaderView(overpassReader, args.getInputQuery());
		} else {
			return new OSMDataReaderView(dataReader);
		}

	}

}
