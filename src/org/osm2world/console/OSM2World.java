package org.osm2world.console;

import static org.osm2world.core.GlobalValues.VERSION_STRING;

import java.io.IOException;

import javax.swing.UIManager;

import org.osm2world.console.CLIArgumentsUtil.ProgramMode;
import org.osm2world.core.GlobalValues;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.MessageManager;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

import uk.co.flamingpenguin.jewel.cli.ArgumentValidationException;
import uk.co.flamingpenguin.jewel.cli.CliFactory;

/**
 * main class of the OSM2World console application
 */
public class OSM2World {

	public static void main(String[] unparsedArgs) {
		
		ProgramMode programMode;
		CLIArguments args = null;
		
		if (unparsedArgs.length > 0) {
			
			try {
				args = CliFactory.parseArguments(CLIArguments.class, unparsedArgs);
			} catch (ArgumentValidationException e) {
				System.err.println(e.getMessage());
				System.exit(1);
			}
			
			if (!CLIArgumentsUtil.isValid(args)) {
				System.err.println(CLIArgumentsUtil.getErrorString(args));
				System.exit(1);
			}
			
			programMode = CLIArgumentsUtil.getProgramMode(args);
			
		} else {
		 
			System.out.println("No parameters, running graphical interface.\n"
					+ "If you want to use the command line, use the --help"
					+ " parameter for a list of available parameters.");
			programMode = ProgramMode.GUI;
			
		}
		
		switch (programMode) {
		
		case HELP:
			//parser.printHelp();
			System.out.println(
					CliFactory.createCli(CLIArguments.class).getHelpMessage()
					+ "\n\nFor more information, see " + GlobalValues.WIKI_URI);
			break;
			
		case VERSION:
			System.out.println("OSM2World " + VERSION_STRING);
			break;
		
		case GUI:
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch(Exception e) {
				System.out.println("Error setting native look and feel: " + e);
			}
			new ViewerFrame(new Data(), new MessageManager(), new RenderOptions())
				.setVisible(true);
			break;
			
		case CONVERT:
			try {
				Output.output(args);
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
			
		}
		
	}
	
}
