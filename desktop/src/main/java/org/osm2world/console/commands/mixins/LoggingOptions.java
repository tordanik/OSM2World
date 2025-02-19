package org.osm2world.console.commands.mixins;

import java.io.File;

import javax.annotation.Nullable;

import picocli.CommandLine;

public class LoggingOptions {

	@CommandLine.Option(names = {"--logDir"}, description = "output directory for log files", paramLabel = "<path>")
	public @Nullable File logDir;

}
