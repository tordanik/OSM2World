package org.osm2world.console.commands;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import org.osm2world.console.commands.mixins.LoggingOptions;
import org.osm2world.console.commands.mixins.MetadataOptions;

import picocli.CommandLine;

@CommandLine.Command(name = "params", description = "Run 'convert' with parameters from a text file or directory.")
public class ParamsCommand implements Callable<Integer> {

	@CommandLine.Parameters(arity = "1..*", description = "files containing one set of parameters per line, " +
			"or a directory containing an arbitrary amount of such files. " +
			"New files added to such a directory while OSM2World is running will be considered as well.")
	List<File> paths;

	@CommandLine.Option(names = "--deleteProcessedFiles",
			description = "delete parameter files after they have been executed")
	boolean deleteProcessedFiles = false;

	@CommandLine.Mixin
	private LoggingOptions loggingOptions;

	@CommandLine.Mixin
	private MetadataOptions metadataOptions;

	@Override
	public Integer call() throws Exception {
		System.err.println("The params subcommand has not been implemented yet." +
				" Please use the legacy command line interface for now.");
		return 1;
	}

}
