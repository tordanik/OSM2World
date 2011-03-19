package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.osm2world.viewer.model.RenderOptions;

public class ShowCameraConfigurationAction extends AbstractAction {

	private final RenderOptions renderOptions;
	
	public ShowCameraConfigurationAction(RenderOptions renderOptions) {
		super("Current camera configuration");
		this.renderOptions = renderOptions;
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		
		//TODO: show values
		
		JOptionPane.showMessageDialog(null,
				"posLat = \n"
				+ "posLon = \n"
				+ "posEle = \n"
				+ "lookAtLat = \n"
				+ "lookAtLon = \n"
				+ "lookAtEle = \n");
	}
	
}
