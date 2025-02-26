package org.osm2world.console.commands;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.HelpCommand;

import java.util.concurrent.Callable;

import org.osm2world.util.GlobalValues;

@Command(name = "osm2world",
		mixinStandardHelpOptions = true,
		version = GlobalValues.VERSION_STRING,
		description = "Creates 3D models from OpenStreetMap data",
		subcommands = {HelpCommand.class, ConvertCommand.class, GuiCommand.class, ParamsCommand.class})
public class RootCommand implements Callable<Integer> {

	@Override
	public Integer call() {
		return 0;
	}

}
