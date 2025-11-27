package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.osm2world.math.geo.TileNumber;
import org.osm2world.osm.creation.*;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.RecentFilesUpdater;
import org.osm2world.viewer.view.ViewerFrame;

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
			openOSMFile(osmFile, true);
		}

	}

	public void openOSMFile(File osmFile, boolean resetCamera) {

		boolean mbtiles = osmFile.getName().endsWith(".mbtiles");
		boolean geodesk = osmFile.getName().endsWith(".gol");
		boolean json = osmFile.getName().endsWith(".json");

		if (!mbtiles && !geodesk) {

			if (osmFile.length() > 1e7) {

				String[] options = new String[] {"Try anyway", "Cancel"};

				int answer = JOptionPane.showOptionDialog(viewerFrame,
						"""
								The input file is probably too big.
								This viewer can only handle relatively small areas well.""",
						"Large input file size",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE,
						null, options, options[1]);

				if (answer != JOptionPane.OK_OPTION) return;

			}

			OSMDataReader reader = json ? new JsonFileReader(osmFile) : new OSMFileReader(osmFile);
			loadOSMData(new OSMDataReaderView(reader), resetCamera);

		} else {

			try {

				String tileString = JOptionPane.showInputDialog("Enter a tile number (such as \"13,1234,4321\")");
				if (tileString == null) return;
				TileNumber tileNumber = new TileNumber(tileString);
				OSMDataReader reader = mbtiles
						? new MbtilesReader(osmFile)
						: new GeodeskReader(osmFile);
				loadOSMData(new OSMDataReaderView(reader, tileNumber), true);

			} catch (IllegalArgumentException e) {
				JOptionPane.showMessageDialog(viewerFrame, "Invalid input: " + e.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
			}

		}

		RecentFilesUpdater.addRecentFile(osmFile);

	}

	private File askFile() {

		JFileChooser chooser = new JFileChooser(lastPath);
		chooser.setDialogTitle("Open OSM file");
		chooser.setFileFilter(new FileNameExtensionFilter(
				"OpenStreetMap data files", "osm", "gz", "bz2", "pbf", "json", "mbtiles", "gol"));

		int returnVal = chooser.showOpenDialog(null);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File selectedFile = chooser.getSelectedFile();
			lastPath = selectedFile.getAbsoluteFile().getParentFile();
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
