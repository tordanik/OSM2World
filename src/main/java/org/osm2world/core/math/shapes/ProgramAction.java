package org.osm2world.core.math.shapes;
import org.apache.commons.configuration.Configuration;
import org.osm2world.console.CLIArguments;
import org.osm2world.console.CLIArgumentsGroup;

import java.io.File;
import java.io.IOException;

/**
 * "Replace conditional with polymorphism" refactoring done.
 */
public interface ProgramAction {
    void doAction(CLIArguments representativeArgs, Configuration config, CLIArgumentsGroup argumentsGroup, File configFile) throws IOException;
}


