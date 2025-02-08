package org.osm2world.viewer;

import javax.swing.*;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.viewer.view.ViewerFrame;

public class Viewer {

	private Viewer(String[] args) {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			System.out.println("Error setting native look and feel: " + e);
		}

		new ViewerFrame(new O2WConfig(), null, null, null).setVisible(true);

	}

}
