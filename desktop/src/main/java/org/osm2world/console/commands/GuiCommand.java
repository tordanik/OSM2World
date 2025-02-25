package org.osm2world.console.commands;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.swing.*;

import org.osm2world.console.commands.mixins.ConfigOptions;
import org.osm2world.scene.mesh.LevelOfDetail;
import org.osm2world.viewer.view.ViewerFrame;

import picocli.CommandLine;

@CommandLine.Command(name = "gui", description = "Start the graphical user interface.")
public class GuiCommand implements Callable<Integer> {

	@CommandLine.Mixin
	ConfigOptions configOptions = new ConfigOptions();

	@Override
	public Integer call() {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			System.out.println("Error setting native look and feel: " + e);
		}

		File input = null; // TODO support input
		LevelOfDetail lod = null; // TODO support lod

		var viewerFrame = new ViewerFrame(configOptions.getO2WConfig(Map.of()), lod, configOptions.getConfigFiles(), input);
		viewerFrame.setVisible(true);

		return -1; // tells the main method not to call System.exit

	}

}
