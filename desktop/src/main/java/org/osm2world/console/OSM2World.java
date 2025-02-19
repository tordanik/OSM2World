package org.osm2world.console;

import org.osm2world.console.commands.GuiCommand;
import org.osm2world.console.commands.RootCommand;
import org.osm2world.console.commands.converters.LodConverter;
import org.osm2world.console.legacy.LegacyCLI;
import org.osm2world.math.geo.LatLon;
import org.osm2world.scene.mesh.LevelOfDetail;
import org.osm2world.util.Resolution;

import picocli.CommandLine;

/**
 * main class of the OSM2World console application
 */
public class OSM2World {

	public static void main(String[] args) throws Exception {

		/* deal with a missing subcommand */

		if (args.length == 0) {

			System.out.println("No subcommand or arguments, running graphical interface.\n"
					+ "If you want to use the command line, use the help"
					+ " subcommand for a list of available options.");

			new GuiCommand().call();

		} else if (args[0].startsWith("-")) {

			System.out.println("The first argument is not a subcommand," +
					" falling back to legacy command line parsing");

			LegacyCLI.main(args);

		} else {

			/* run the command line normally */

			CommandLine commandLine = new CommandLine(new RootCommand());
			commandLine.setCaseInsensitiveEnumValuesAllowed(true);
			commandLine.registerConverter(Resolution.class, Resolution::new);
			commandLine.registerConverter(LevelOfDetail.class, new LodConverter());
			commandLine.registerConverter(LatLon.class, LatLon::new);

			int exitCode = commandLine.execute(args);

			if (exitCode >= 0) {
				System.exit(exitCode);
			}

		}

	}

}