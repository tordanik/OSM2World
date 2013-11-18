package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import static org.osm2world.core.GlobalValues.*;

public class AboutAction extends AbstractAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6717063896933933005L;

	public AboutAction() {
		super("About OSM2World");
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		JOptionPane.showMessageDialog(null, "OSM2World\nVersion " + VERSION_STRING
				+ "\n\nFor more information, visit\n"
				+ WIKI_URI + "\n" + OSM2WORLD_URI, 
				"About OSM2World", JOptionPane.INFORMATION_MESSAGE);
	}
	
}
