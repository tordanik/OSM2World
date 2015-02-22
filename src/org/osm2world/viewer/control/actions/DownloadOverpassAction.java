package org.osm2world.viewer.control.actions;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.osm2world.core.ConversionFacade.BoundingBoxSizeException;
import org.osm2world.core.ConversionFacade.Phase;
import org.osm2world.core.ConversionFacade.ProgressListener;
import org.osm2world.core.map_data.creation.LatLonBounds;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.TerrainInterpolator;
import org.osm2world.core.math.InvalidGeometryException;
import org.osm2world.core.osm.creation.OverpassReader;
import org.osm2world.core.util.functions.DefaultFactory;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ProgressDialog;
import org.osm2world.viewer.view.RecentFilesUpdater;
import org.osm2world.viewer.view.ViewerFrame;

public class DownloadOverpassAction extends AbstractAction {

	ViewerFrame viewerFrame;
	Data data;
	RenderOptions renderOptions;

	private File lastPath = null;

	public DownloadOverpassAction(ViewerFrame viewerFrame, Data data, RenderOptions renderOptions) {

		super("Download OSM data");
		putValue(SHORT_DESCRIPTION, "Download OpenStreetMap data from Overpass API");
		putValue(MNEMONIC_KEY, KeyEvent.VK_D);
		
		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;
		
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

		LatLonBounds bounds = askLatLonBounds();

		if (bounds != null) {
			
			new OverpassReader(bounds.getMin(), bounds.getMax());
			
		}

	}

	private LatLonBounds askLatLonBounds() {
		
		double minLat = Double.parseDouble(
				JOptionPane.showInputDialog(viewerFrame, "minLat"));
		double minLon = Double.parseDouble(
				JOptionPane.showInputDialog(viewerFrame, "minLon"));
		double maxLat = Double.parseDouble(
				JOptionPane.showInputDialog(viewerFrame, "maxLat"));
		double maxLon = Double.parseDouble(
				JOptionPane.showInputDialog(viewerFrame, "maxLon"));
		
		return new LatLonBounds(minLat, minLon, maxLat, maxLon);
		
		/*
		
		JDialog dialog = new JDialog(viewerFrame);
		dialog.setTitle("Select data bounds");
		dialog.setSize(600, 300);
		
		JXMapKit map = new JXMapKit();
		map.setDefaultProvider(DefaultProviders.OpenStreetMaps);
		map.setCenterPosition(new GeoPosition(50.746, 7.154));
		map.setZoom(3);
		dialog.add(map, java.awt.BorderLayout.CENTER);
		
		JPanel settingsPanel = new JPanel();
		dialog.add(settingsPanel, java.awt.BorderLayout.EAST);
		BoxLayout settingsPanelLayout = new BoxLayout(settingsPanel, BoxLayout.PAGE_AXIS);
		settingsPanel.setLayout(settingsPanelLayout);
		
		ButtonGroup buttonGroup = new ButtonGroup();
		JRadioButton coordinatesRB = new JRadioButton("Coordinates");
		JRadioButton customQueryRB = new JRadioButton("Custom Query");
		buttonGroup.add(coordinatesRB);
		buttonGroup.add(customQueryRB);
		settingsPanel.add(coordinatesRB);
				
		JTextField minLatField = new JTextField();
		settingsPanel.add(new JLabel("minimum latitude"));
		settingsPanel.add(minLatField);
		JTextField minLonField = new JTextField();
		settingsPanel.add(new JLabel("minimum longitude"));
		settingsPanel.add(minLonField);
		JTextField maxLatField = new JTextField();
		settingsPanel.add(new JLabel("maximum latitude"));
		settingsPanel.add(maxLatField);
		JTextField maxLonField = new JTextField();
		settingsPanel.add(new JLabel("maximum longitude"));
		settingsPanel.add(maxLonField);
		
		settingsPanel.add(customQueryRB);
		
		JTextArea queryArea = new JTextArea();
		settingsPanel.add(queryArea);
				
		dialog.setVisible(true);
		*/
		
	}

	public void openOSMFile(File osmFile, boolean resetCamera) {
		
		OpenOSMThread thread = new OpenOSMThread(osmFile, resetCamera);
		thread.setUncaughtExceptionHandler(
				new ConversionExceptionHandler(viewerFrame));
		thread.start();
		
		RecentFilesUpdater.addRecentFile(osmFile);
		
	}
	
	private class OpenOSMThread extends Thread implements ProgressListener {

		private final File osmFile;
		private final boolean resetCamera;
		
		private ProgressDialog progressDialog;

		public OpenOSMThread(File osmFile, boolean resetCamera) {
			super("OpenOSMThread");
			this.osmFile = osmFile;
			this.resetCamera = resetCamera;
		}

		@Override
		public void run() {

			viewerFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			viewerFrame.setEnabled(false);
		
			boolean failOnLargeBBox = true;
			boolean runAgain = true;
			
			/* check file size */
			
			if (osmFile.length() > 1e7) {
				
				String[] options = new String[] {"Try anyway" ,"Cancel"};
				
				int answer = JOptionPane.showOptionDialog(viewerFrame,
						"The input file is probably too big.\n"
						+ "This viewer can only handle relatively small areas well.\n",
						"Large input file size",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE,
						null, options, options[1]);
				
				runAgain = (answer != 1);
				
			}
			
			/* attempt to open the file */
						
			while (runAgain) {
				
				runAgain = false;
				
				progressDialog = new ProgressDialog(viewerFrame, "Open OSM File");
								
				try {
					
					data.loadOSMFile(osmFile, failOnLargeBBox,
							new DefaultFactory<TerrainInterpolator>(
									renderOptions.getInterpolatorClass()),
							new DefaultFactory<EleConstraintEnforcer>(
									renderOptions.getEnforcerClass()),
							this);
	
					if (resetCamera) {
						new ResetCameraAction(viewerFrame, data, renderOptions).actionPerformed(null);
					}
	
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
					
				} catch (BoundingBoxSizeException e) {
					
					String[] options = new String[] {"Try anyway" ,"Cancel"};
					
					int answer = JOptionPane.showOptionDialog(viewerFrame,
							"The input file covers a large bounding box.\n"
							+ "This viewer can only handle relatively small areas well.\n",
							"Large bounding box",
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.WARNING_MESSAGE,
							null, options, options[1]);
					
					failOnLargeBBox = false;
					runAgain = (answer != 1);
					
				}

				progressDialog.dispose();
				
			}
			
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
