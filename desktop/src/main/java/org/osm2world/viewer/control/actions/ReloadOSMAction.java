package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;

import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

/**
 * reloads the previously opened OSM file
 */
public class ReloadOSMAction extends AbstractAction implements Observer {

	private static final long serialVersionUID = 1162049141590529184L; //generated serialVersionUID
	private final ViewerFrame viewerFrame;
	private final Data data;
	private final RenderOptions renderOptions;

	public ReloadOSMAction(ViewerFrame viewerFrame, Data data, RenderOptions renderOptions) {

		super("Reload OSM file");
		putValue(SHORT_DESCRIPTION, "Reloads the most recently opened OSM file" +
				" and the configuration file");
		putValue(MNEMONIC_KEY, KeyEvent.VK_R);
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
				KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));

		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;

		this.setEnabled(false);

		data.addObserver(this);

	}


	@Override
	public void actionPerformed(ActionEvent arg0) {
		new OpenOSMAction(viewerFrame, data, renderOptions)
				.openOSMFile(data.getOsmFile(), false);
	}

	@Override
	public void update(Observable o, Object arg) {
		this.setEnabled(data.getOsmFile() != null);
	}

}