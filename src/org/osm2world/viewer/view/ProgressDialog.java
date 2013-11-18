package org.osm2world.viewer.view;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

public class ProgressDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1016979607905010257L;
	private final JProgressBar progressBar;
	private final JTextField statusField;
	
	public ProgressDialog(ViewerFrame viewerFrame, String title) {
		
		super(viewerFrame, title);
		
		JPanel panel = new JPanel();
		BorderLayout layout = new BorderLayout();
		panel.setLayout(layout);
		
		progressBar = new JProgressBar();
		panel.add(progressBar, BorderLayout.CENTER);
		
		statusField = new JTextField(50);
		statusField.setEditable(false);
		panel.add(statusField, BorderLayout.SOUTH);

		this.add(panel);
		
		setProgress(null);
		
		this.pack();
		this.setVisible(true);
		
	}
	
	public void setText(String text) {
		statusField.setText(text);
	}
	
	/**
	 * @param progress  progress percentage or null for indeterminate progress
	 */
	public void setProgress(Integer progress) {
		if (progress == null) {
			progressBar.setIndeterminate(true);
		} else {
			progressBar.setIndeterminate(false);
			progressBar.setValue(progress);
		}
	}
	
}
