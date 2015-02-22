package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.osm2world.core.osm.creation.OSMFileReader;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.RecentFilesUpdater;
import org.osm2world.viewer.view.ViewerFrame;

public class OpenOSMAction extends AbstractLoadOSMAction {
	
	private File lastPath = null;

	public OpenOSMAction(ViewerFrame viewerFrame, Data data, RenderOptions renderOptions) {

		super("Open OSM file", viewerFrame, data, renderOptions);
		putValue(SHORT_DESCRIPTION, "Opens a file with OpenStreetMap data");
		putValue(MNEMONIC_KEY, KeyEvent.VK_O);
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
				KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		
		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;
		
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

		File osmFile = askFile();

		if (osmFile != null) {

			openOSMFile(osmFile, true);
			
		}

	}

	public void openOSMFile(File osmFile, boolean resetCamera) {
		
		if (osmFile.length() > 1e7) {
			
			String[] options = new String[] {"Try anyway" ,"Cancel"};
			
			int answer = JOptionPane.showOptionDialog(viewerFrame,
					"The input file is probably too big.\n"
					+ "This viewer can only handle relatively small areas well.\n",
					"Large input file size",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE,
					null, options, options[1]);
			
			if (answer != JOptionPane.OK_OPTION) return;
			
		}
		
		loadOSMFile(new OSMFileReader(osmFile), resetCamera);
		
		RecentFilesUpdater.addRecentFile(osmFile);
		
	}

	private File askFile() {

		JFileChooser chooser = new JFileChooser(lastPath);
		chooser.setDialogTitle("Open OSM file");
		chooser.setFileFilter(new FileNameExtensionFilter(
				"OpenStreetMap data files", "osm", "gz", "bz2", "pbf"));

		int returnVal = chooser.showOpenDialog(null);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File selectedFile = chooser.getSelectedFile();
			lastPath = selectedFile.getParentFile();
			return selectedFile;
		} else {
			return null;
		}

		//		/* alternative implementation using FileDialog */
		//
		//		FileDialog dialog =
		//            new FileDialog(viewerFrame, "Open OSM file", FileDialog.LOAD);
		//
		//		dialog.setVisible(true);
		//
		//		return new File(dialog.getFile());

	}
	
}
