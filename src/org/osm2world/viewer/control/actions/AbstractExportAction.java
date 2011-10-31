package org.osm2world.viewer.control.actions;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Observable;
import java.util.Observer;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.MessageManager;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ProgressDialog;
import org.osm2world.viewer.view.ViewerFrame;

public abstract class AbstractExportAction
	extends AbstractAction implements Observer {

	protected final ViewerFrame viewerFrame;
	protected final Data data;
	protected final MessageManager messageManager;
	protected final RenderOptions renderOptions;
	
	private File lastPath = null;

	protected AbstractExportAction(String name,
			ViewerFrame viewerFrame, Data data,
			MessageManager messageManager,
			RenderOptions renderOptions) {
		
		super(name);
		
		this.viewerFrame = viewerFrame;
		this.messageManager = messageManager;
		this.data = data;
		this.renderOptions = renderOptions;
		
		setEnabled(false);
		data.addObserver(this);
		
	}
	
	@Override
	public void update(Observable o, Object arg) {
		setEnabled(data.getConversionResults() != null);
	}

	protected boolean chooseDirectory() { return false; }
	
	abstract protected FileNameExtensionFilter getFileNameExtensionFilter();

	abstract protected void performExport(File file);

	@Override
	public void actionPerformed(ActionEvent arg0) {
	
		File file = askFile();
		
		if (file != null) {
			new ExportFileThread(file).start();
		}
		
	}
	
	private File askFile() {
		
		JFileChooser chooser = new JFileChooser(lastPath);
		chooser.setDialogTitle("Export file");
		
		if (chooseDirectory()) {
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		} else {
			chooser.setFileFilter(getFileNameExtensionFilter());
		}
		
		int returnVal = chooser.showSaveDialog(null);
	
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			
			File selectedFile = chooser.getSelectedFile();
			
			lastPath = selectedFile.getParentFile();
			
			/* make sure that file uses correct extension */
					
			if (getFileNameExtensionFilter() != null &&
					getFileNameExtensionFilter().getExtensions().length == 1) {
				
				String extension = getFileNameExtensionFilter().getExtensions()[0];
								
				if (!selectedFile.getPath().endsWith("." + extension)) {
					selectedFile = new File(selectedFile.getPath() + "." + extension);
				}
				
			}
			
			return selectedFile;
			
		} else {
			return null;
		}
		
	}
	private class ExportFileThread extends Thread {

		private final File file;
		private ProgressDialog progressDialog;

		public ExportFileThread(File file) {
			super("ExportFileThread");
			this.file = file;
		}

		@Override
		public void run() {

			viewerFrame.setCursor(Cursor
					.getPredefinedCursor(Cursor.WAIT_CURSOR));
			viewerFrame.setEnabled(false);

			String progressDescription = "Export file";
			if (getFileNameExtensionFilter() != null) {
				progressDescription += " of type "
						+ getFileNameExtensionFilter().getDescription();
			}
			
			progressDialog = new ProgressDialog(viewerFrame, progressDescription);
			progressDialog.setProgress(null);
			progressDialog.setText("Writing file: " + file.getAbsolutePath());
			
			performExport(file);

			progressDialog.dispose();

			viewerFrame.setCursor(Cursor
					.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			viewerFrame.setEnabled(true);

		}

	}

}
