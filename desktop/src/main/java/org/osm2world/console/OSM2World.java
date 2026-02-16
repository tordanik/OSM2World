package org.osm2world.console;

import java.util.List;

import org.osm2world.console.commands.GuiCommand;
import org.osm2world.console.commands.RootCommand;
import org.osm2world.console.commands.converters.LatLonBoundsConverter;
import org.osm2world.console.commands.converters.LodConverter;
import org.osm2world.console.commands.converters.TileBoundsConverter;
import org.osm2world.console.legacy.LegacyCLI;
import org.osm2world.math.geo.*;
import org.osm2world.scene.mesh.LevelOfDetail;
import org.osm2world.util.Resolution;
import org.osm2world.util.platform.image.ImageImplementationJvm;
import org.osm2world.util.platform.json.JsonImplementationJvm;
import org.osm2world.util.platform.uri.HttpUriImplementationJvm;

import picocli.CommandLine;

/**
 * main class of the OSM2World console application
 */
public class OSM2World {

	static {
		HttpUriImplementationJvm.register();
		JsonImplementationJvm.register();
		ImageImplementationJvm.register();
	}

	public static void main(String[] args) throws Exception {
		main(args, true);
	}

	public static void main(String[] args, boolean exit) throws Exception {

		/* deal with a missing subcommand */

		if (args.length == 0) {

			System.out.println("No subcommand or arguments, running graphical interface.\n"
					+ "If you want to use the command line, use the help"
					+ " subcommand for a list of available options.");

			new GuiCommand().call();

		} else if (args[0].startsWith("-") && !List.of("--version", "-V", "--help", "-h").contains(args[0])) {

			System.out.println("The first argument is not a subcommand," +
					" falling back to legacy command line parsing");

			LegacyCLI.main(args);

		} else {

			/* run the command line normally */

			CommandLine commandLine = buildCommandLine(new RootCommand());

			int exitCode = commandLine.execute(args);

			if (exit && exitCode >= 0) {
				System.exit(exitCode);
			}

		}

	}

	public static CommandLine buildCommandLine(Object command) {
		CommandLine commandLine = new CommandLine(command);
		commandLine.setCaseInsensitiveEnumValuesAllowed(true);
		commandLine.registerConverter(LatLon.class, LatLon::new);
		commandLine.registerConverter(LatLonBounds.class, new LatLonBoundsConverter());
		commandLine.registerConverter(LatLonEle.class, LatLonEle::new);
		commandLine.registerConverter(LevelOfDetail.class, new LodConverter());
		commandLine.registerConverter(Resolution.class, Resolution::new);
		commandLine.registerConverter(TileBounds.class, new TileBoundsConverter());
		commandLine.registerConverter(TileNumber.class, TileNumber::new);
		return commandLine;
	}

}