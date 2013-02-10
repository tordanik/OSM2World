package org.osm2world.viewer;

import javax.swing.UIManager;

import org.apache.commons.configuration.BaseConfiguration;
import org.osm2world.viewer.view.ViewerFrame;

public class Viewer {
	
	public static void main(String[] args) {
		new Viewer(args);
	}
	
	private Viewer(String[] args) {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			System.out.println("Error setting native look and feel: " + e);
		}
		
		new ViewerFrame(new BaseConfiguration(), null, null).setVisible(true);
		
	}
	
}
