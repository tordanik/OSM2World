package org.osm2world.console.legacy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.CliFactory;


public class CLIArgumentsGroupTest {

	@Test
	public void testIsCompatible() throws ArgumentValidationException {

		/* test some basic usage */

		CLIArguments cliArgs1 = CliFactory.parseArguments(CLIArguments.class,
				"-i", "testFile.osm", "-o", "foobar.png");
		CLIArguments cliArgs2 = CliFactory.parseArguments(CLIArguments.class,
				"-i", "testFile.osm", "-o", "bazbar.pov");

		assertTrue(CLIArgumentsGroup.isCompatible(cliArgs1, cliArgs2));

		CLIArguments cliArgs3 = CliFactory.parseArguments(CLIArguments.class,
				"-i", "testFile2.osm", "-o", "foobar.png");

		assertFalse(CLIArgumentsGroup.isCompatible(cliArgs1, cliArgs3));

		/* test tileserver-style commands */

		CLIArguments cliArgsA1 = CliFactory.parseArguments(CLIArguments.class,
				"--config", "osm2world.config", "-i", "/o2wmaps/input/old/13_4231_2777.pbf", "-o", "/tmp/n_ogltile_4231_2777.ppm", "--resolution", "8192,4096", "--oview.tiles", "13,4231,2777", "--oview.from", "S", "--logDir", "/tmp/logs/");
		CLIArguments cliArgsA2 = CliFactory.parseArguments(CLIArguments.class,
				"--config", "osm2world.config", "-i", "/o2wmaps/input/old/13_4231_2777.pbf", "-o", "/tmp/n_ogltile_4231_2777.ppm", "--resolution", "8192,4096", "--oview.tiles", "13,4231,2777", "--oview.from", "N", "--logDir", "/tmp/logs/");

		assertTrue(CLIArgumentsGroup.isCompatible(cliArgsA1, cliArgsA2));

	}

}
