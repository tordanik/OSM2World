package org.osm2world.viewer.control.actions;

import static java.lang.Math.ceil;
import static org.osm2world.viewer.model.Data.BoundingBoxSizeException;

import java.awt.*;
import java.io.IOException;
import java.io.Serial;

import javax.swing.*;

import org.osm2world.conversion.ProgressListener;
import org.osm2world.map_elevation.creation.EleCalculator;
import org.osm2world.map_elevation.creation.TerrainInterpolator;
import org.osm2world.osm.creation.OSMDataReaderView;
import org.osm2world.util.exception.InvalidGeometryException;
import org.osm2world.util.functions.DefaultFactory;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ProgressDialog;
import org.osm2world.viewer.view.ViewerFrame;


public abstract class AbstractLoadOSMAction extends AbstractAction {

	@Serial
	private static final long serialVersionUID = 1L;

	protected ViewerFrame viewerFrame;
	protected Data data;
	protected RenderOptions renderOptions;

	protected AbstractLoadOSMAction(String label, ViewerFrame viewerFrame, Data data, RenderOptions renderOptions) {

		super(label);

		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;

	}

	protected void loadOSMData(OSMDataReaderView dataReader, boolean resetCamera) {

		try {
			data.reloadConfig(renderOptions);
		} catch (Exception e) {

			JOptionPane.showMessageDialog(viewerFrame,
					"Could not reload the properties configuration file:\n"
							+ e.getMessage(),
					"Error reloading configuration",
					JOptionPane.WARNING_MESSAGE);

			System.err.println(e);

		}

		LoadOSMThread thread = new LoadOSMThread(dataReader, resetCamera);

		thread.setUncaughtExceptionHandler((Thread t, Throwable e) -> {
			if (SwingUtilities.isEventDispatchThread()) {
				showExceptionAndQuit(viewerFrame, t, e);
			} else {
				SwingUtilities.invokeLater(() -> showExceptionAndQuit(viewerFrame, t, e));
			}
		});

		thread.start();

	}

	private static void showExceptionAndQuit(ViewerFrame viewerFrame, Thread t, Throwable e) {

		e.printStackTrace();

		String msg = String.format("Unexpected problem on thread %s:\n%s\n\n"
						+ "OSM2World will be closed now.\n\nLocation:\n%s\n%s",
				t.getName(), e, e.getStackTrace()[0], e.getStackTrace()[1]);
		JOptionPane.showMessageDialog(viewerFrame, msg, "Error", JOptionPane.ERROR_MESSAGE);

		System.exit(1);

	}

	private class LoadOSMThread extends Thread implements ProgressListener {

		private final OSMDataReaderView dataReader;
		private final boolean resetCamera;

		private ProgressDialog progressDialog;

		public LoadOSMThread(OSMDataReaderView dataReader, boolean resetCamera) {
			super("OpenOSMThread");
			this.dataReader = dataReader;
			this.resetCamera = resetCamera;
		}

		@Override
		public void run() {

			viewerFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			viewerFrame.setEnabled(false);

			boolean failOnLargeBBox = true;
			boolean runAgain = true;

			/* attempt to open the file */

			while (runAgain) {

				runAgain = false;

				progressDialog = new ProgressDialog(viewerFrame, "Open OSM File");

				try {

					data.loadOSMData(dataReader, failOnLargeBBox,
							new DefaultFactory<TerrainInterpolator>(
									renderOptions.getInterpolatorClass()),
							new DefaultFactory<EleCalculator>(
									renderOptions.getEleCalculatorClass()),
							this);

					if (resetCamera) {
						new ResetCameraAction(viewerFrame, data, renderOptions).actionPerformed(null);
					}

				} catch (IOException e) {

					JOptionPane.showMessageDialog(viewerFrame,
							e + "\nCause: " + (e.getCause() == null ? "unknown" : e.getCause()),
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
		public void updateProgress(Phase currentPhase, double progress) {

			String text = switch (currentPhase) {
				case MAP_DATA -> "1/5: Organize information from .osm file...";
				case REPRESENTATION -> "2/5: Choose visual representations for OSM objects...";
				case ELEVATION -> "3/5: Guess elevations from available information...";
				case TERRAIN -> "4/5: Generate terrain...";
				case OUTPUT -> "5/5: Represent objects by 3D primitives...";
				case FINISHED -> "Conversion complete";
			};

			progressDialog.setProgress((int) ceil(progress * 100));
			progressDialog.setText(text);

		}

	}

}
