package org.osm2world.viewer;

import javax.swing.UIManager;

import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.MessageManager;
import org.osm2world.viewer.model.RenderOptions;
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
		
		new ViewerFrame(new Data(), new MessageManager(), new RenderOptions(),
				null, null).setVisible(true);
		
	}
	
}
