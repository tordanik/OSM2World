package org.osm2world.viewer.control.actions;

import org.osm2world.core.osm.creation.GeodeskReader;
import org.osm2world.core.osm.creation.MbtilesReader;
import org.osm2world.core.osm.creation.OSMDataReader;
import org.osm2world.core.osm.creation.OSMFileReader;
import org.osm2world.core.target.common.rendering.TileNumber;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.RecentFilesUpdater;
import org.osm2world.viewer.view.ViewerFrame;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

public class OpenOSMAction extends AbstractLoadOSMAction {

	private static final long serialVersionUID = -3092902524926341197L;

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

			boolean mbtiles = osmFile.getName().endsWith(".mbtiles");
			boolean geodesk = osmFile.getName().endsWith(".gol");

			if (!mbtiles && !geodesk) {
				openOSMFile(osmFile, true);
			} else {

				try {

					String tileString = JOptionPane.showInputDialog("Enter a tile number (such as \"13,1234,4321\")");
					TileNumber tileNumber = new TileNumber(tileString);
					OSMDataReader reader = mbtiles
							? new MbtilesReader(osmFile, tileNumber)
							: new GeodeskReader(osmFile, tileNumber.bounds());
					loadOSMData(reader, true);

				} catch (IllegalArgumentException e) {
					JOptionPane.showMessageDialog(viewerFrame, "Invalid input: " + e.getMessage(),
							"Error", JOptionPane.ERROR_MESSAGE);
				}

			}

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

		try {
			loadOSMData(new OSMFileReader(osmFile), resetCamera);
		} catch (IOException e) {

			String msg = "File not found:\n" + osmFile;

			JOptionPane.showMessageDialog(viewerFrame, msg,
					"Error", JOptionPane.ERROR_MESSAGE);

		}

		RecentFilesUpdater.addRecentFile(osmFile);

	}

	private File askFile() {

		JFileChooser chooser = new JFileChooser(lastPath);
		chooser.setDialogTitle("Open OSM file");
		chooser.setFileFilter(new FileNameExtensionFilter(
				"OpenStreetMap data files", "osm", "gz", "bz2", "pbf", "mbtiles", "gol"));

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
