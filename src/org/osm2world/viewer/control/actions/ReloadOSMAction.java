package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

/**
 * reloads the previously opened OSM file
 */
public class ReloadOSMAction extends AbstractAction implements Observer {

	private final ViewerFrame viewerFrame;
	private final Data data;
	private final RenderOptions renderOptions;
	
	public ReloadOSMAction(ViewerFrame viewerFrame, Data data,
			RenderOptions renderOptions) {
		
		super("Reload OSM file");
		putValue(SHORT_DESCRIPTION, "Reloads the most recently opened OSM file");
		putValue(MNEMONIC_KEY, KeyEvent.VK_R);
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
				KeyEvent.VK_R, ActionEvent.CTRL_MASK));
		
		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;
		
		this.setEnabled(false);
		
		data.addObserver(this);
		
	}
	

	@Override
	public void actionPerformed(ActionEvent arg0) {
		
		new OpenOSMAction(viewerFrame, data, renderOptions)
				.openOSMFile(data.getOsmFile());
		
	}
	
	@Override
	public void update(Observable o, Object arg) {
		this.setEnabled(data.getOsmFile() != null);
	}
	
}
