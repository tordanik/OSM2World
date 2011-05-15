package org.osm2world.viewer.control.actions;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.osm2world.core.ConversionFacade.Phase;
import org.osm2world.core.ConversionFacade.ProgressListener;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ProgressDialog;
import org.osm2world.viewer.view.ViewerFrame;

public class OpenOSMAction extends AbstractAction {

	ViewerFrame viewerFrame;
	Data data;
	RenderOptions renderOptions;

	private File lastPath = null;

	public OpenOSMAction(ViewerFrame viewerFrame, Data data, RenderOptions renderOptions) {

		super("Open OSM file");
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

			openOSMFile(osmFile);

		}

	}

	public void openOSMFile(File osmFile) {
		OpenOSMThread thread = new OpenOSMThread(osmFile);
		thread.setUncaughtExceptionHandler(
				new ConversionExceptionHandler(viewerFrame));
		thread.start();
	}

	private File askFile() {

		JFileChooser chooser = new JFileChooser(lastPath);
		chooser.setDialogTitle("Open OSM file");
		chooser.setFileFilter(new FileNameExtensionFilter("OpenStreetMap data files", "osm", "gz", "bz2"));

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

	private class OpenOSMThread extends Thread implements ProgressListener {

		private final File osmFile;
		private ProgressDialog progressDialog;

		public OpenOSMThread(File osmFile) {
			super("OpenOSMThread");
			this.osmFile = osmFile;
		}

		@Override
		public void run() {

			viewerFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			viewerFrame.setEnabled(false);
			
			progressDialog = new ProgressDialog(viewerFrame, "Open OSM File");
			
			try {

				data.loadOSMFile(osmFile, this);

				new ResetCameraAction(viewerFrame, data, renderOptions).actionPerformed(null);

			} catch (IOException e) {
				JOptionPane.showMessageDialog(viewerFrame,
						e.toString() + "\nCause: " +
						(e.getCause() == null ? "unknown" : e.getCause()),
						"Could not open OSM file", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			} catch (InvalidGeometryException e) {
				JOptionPane.showMessageDialog(viewerFrame,
						"The OSM data contains broken geometry.\n"
						+ "Make sure you are not using an extract with"
						+ " incomplete areas!\nIf you are e.g. using Osmosis,"
						+ " you need to use its completeWays=yes parameter.\n"
						+ "See command line output for error details.",
						"Could not perform conversion", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}

			progressDialog.dispose();
			
			viewerFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			viewerFrame.setEnabled(true);

		}

		@Override
		public void updatePhase(Phase newPhase) {
			switch (newPhase) {
			case MAP_DATA:
				progressDialog.setProgress(0);
				progressDialog.setText("1/5: Organize information from .osm file...");
				break;
			case REPRESENTATION:
				progressDialog.setProgress(20);
				progressDialog.setText("2/5: Choose visual representations for OSM objects...");
				break;
			case ELEVATION:
				progressDialog.setProgress(40);
				progressDialog.setText("3/5: Guess elevations from available information...");
				break;
			case TERRAIN:
				progressDialog.setProgress(60);
				progressDialog.setText("4/5: Generate terrain...");
				break;
			case FINISHED:
				progressDialog.setProgress(80);
				progressDialog.setText("5/5: Represent objects by 3D primitives...");
				break;
			}

		}

	}
	
	private static class ConversionExceptionHandler
			implements UncaughtExceptionHandler {
		
		private final ViewerFrame viewerFrame;
		
		public ConversionExceptionHandler(ViewerFrame viewerFrame) {
			this.viewerFrame = viewerFrame;
		}
		
		@Override
		public void uncaughtException(final Thread t, final Throwable e) {
			if (SwingUtilities.isEventDispatchThread()) {
				showException(t, e);
			} else {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						showException(t, e);
					}
				});
			}
			
		}
		
		private void showException(Thread t, Throwable e) {
			
			// TODO log
			e.printStackTrace();
			
			String msg = String.format(
					"Unexpected problem on thread %s:\n%s\n\n"
					+ "OSM2World will be closed now.\n\nLocation:\n%s\n%s",
					t.getName(), e.toString(),
					e.getStackTrace()[0], e.getStackTrace()[1]);
			
			JOptionPane.showMessageDialog(viewerFrame, msg,
					"Error", JOptionPane.ERROR_MESSAGE);
			
			System.exit(1);
			
		}
		
	}
	
}
