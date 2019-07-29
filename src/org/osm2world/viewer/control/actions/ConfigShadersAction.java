package org.osm2world.viewer.control.actions;

import org.apache.commons.configuration.Configuration;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.apache.commons.configuration.Configuration;

import org.osm2world.viewer.view.ViewerFrame;
import org.osm2world.viewer.view.ShaderDialog;

public class ConfigShadersAction extends AbstractAction {
	private final ViewerFrame frame;
	private final Configuration config;
	
	//private static final long serialVersionUID = 9195787581575616092L; //generated serialVersionUID

	public ConfigShadersAction(ViewerFrame frame, Configuration config) {
		super("Shaders");
		this.frame = frame;
		this.config = config;
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		ShaderDialog dialog = new ShaderDialog(frame, config);
	}
	
}
