package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Observable;
import java.util.Observer;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
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
	private final File configFile;

	public ReloadOSMAction(ViewerFrame viewerFrame, Data data,
			RenderOptions renderOptions, File configFile) {

		super("Reload OSM file");
		putValue(SHORT_DESCRIPTION, "Reloads the most recently opened OSM file" +
				" and the configuration file");
		putValue(MNEMONIC_KEY, KeyEvent.VK_R);
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
				KeyEvent.VK_R, ActionEvent.CTRL_MASK));

		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;
		this.configFile = configFile;

		this.setEnabled(false);

		data.addObserver(this);

	}


	@Override
	public void actionPerformed(ActionEvent arg0) {

		/* reload config file */

		if (configFile != null) {

			try {

				PropertiesConfiguration fileConfig = new PropertiesConfiguration();
				fileConfig.setListDelimiter(';');
				fileConfig.load(configFile);
				data.setConfig(fileConfig);

			} catch (ConfigurationException e) {

				JOptionPane.showMessageDialog(viewerFrame,
						"Could not reload the properties configuration file:\n"
						+ e.getMessage(),
						"Error reloading configuration",
						JOptionPane.WARNING_MESSAGE);

				System.err.println(e);

			}

		}

		/* reload OSM file */

		new OpenOSMAction(viewerFrame, data, renderOptions)
				.openOSMFile(data.getOsmFile(), false);

	}

	@Override
	public void update(Observable o, Object arg) {
		this.setEnabled(data.getOsmFile() != null);
	}

}
